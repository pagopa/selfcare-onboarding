package it.pagopa.selfcare.onboarding.service;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.product.entity.ContractStorage;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.io.File;
import java.util.*;

import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_FIELD_LIST;
import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_WORKS_FIELD_LIST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class OnboardingServiceTest {

    @InjectMock
    OnboardingRepository onboardingRepository;
    @InjectMock
    TokenRepository tokenRepository;
    @RestClient
    @InjectMock
    UserApi userRegistryApi;
    @InjectMock
    NotificationService notificationService;
    @InjectMock
    ContractService contractService;
    @InjectMock
    ProductService productService;

    @Inject
    OnboardingService onboardingService;

    final String productId = "productId";

    private Onboarding createOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboarding.getId());
        onboarding.setProductId(productId);
        onboarding.setUsers(List.of());
        Institution institution = new Institution();
        institution.setDescription("description");
        institution.setInstitutionType(InstitutionType.PA);
        onboarding.setInstitution(institution);
        onboarding.setUserRequestUid("example-uid");
        return onboarding;
    }

    private UserResource createUserResource(){
        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        CertifiableFieldResourceOfstring resourceOfName = new CertifiableFieldResourceOfstring();
        resourceOfName.setCertification(CertifiableFieldResourceOfstring.CertificationEnum.NONE);
        resourceOfName.setValue("name");
        userResource.setName(resourceOfName);

        CertifiableFieldResourceOfstring resourceOfSurname = new CertifiableFieldResourceOfstring();
        resourceOfSurname.setCertification(CertifiableFieldResourceOfstring.CertificationEnum.NONE);
        resourceOfSurname.setValue("surname");
        userResource.setFamilyName(resourceOfSurname);
        return userResource;
    }

    @Test
    void getOnboarding() {
        Onboarding onboarding = createOnboarding();
        when(onboardingRepository.findByIdOptional(any())).thenReturn(Optional.of(onboarding));

        Optional<Onboarding> actual = onboardingService.getOnboarding(onboarding.getId());
        assertTrue(actual.isPresent());
        assertEquals(onboarding.getId(), actual.get().getId());
    }

    @Test
    void createContract_shouldThrowIfManagerNotfound() {
        Onboarding onboarding = createOnboarding();
        OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);
        assertThrows(GenericOnboardingException.class, () -> onboardingService.createContract(onboardingWorkflow));
    }

    @Test
    void createContract_InstitutionContractMappings() {

        UserResource userResource = createUserResource();

        Onboarding onboarding = createOnboarding();
        User manager = new User();
        manager.setId(userResource.getId().toString());
        manager.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(manager));

        Product product = createDummyProduct();
        /* add contract mapping */
        Map<InstitutionType, ContractStorage> contractStorageMap = new HashMap<>();
        ContractStorage contractStorage = new ContractStorage();
        contractStorage.setContractTemplatePath("setContractTemplatePath");
        contractStorageMap.put(onboarding.getInstitution().getInstitutionType(), contractStorage);
        product.setInstitutionContractMappings(contractStorageMap);

        when(userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST,manager.getId()))
                .thenReturn(userResource);

        when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(product);

        OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);
        onboardingService.createContract(onboardingWorkflow);

        Mockito.verify(userRegistryApi, Mockito.times(1))
                .findByIdUsingGET(USERS_WORKS_FIELD_LIST,manager.getId());

        Mockito.verify(productService, Mockito.times(1))
                .getProductIsValid(onboarding.getProductId());

        ArgumentCaptor<String> captorTemplatePath = ArgumentCaptor.forClass(String.class);
        Mockito.verify(contractService, Mockito.times(1))
                .createContractPDF(captorTemplatePath.capture(), any(), any(), any(), any());
        assertEquals(captorTemplatePath.getValue(), contractStorage.getContractTemplatePath());
    }

    private static OnboardingWorkflow getOnboardingWorkflowInstitution(Onboarding onboarding) {
        return new OnboardingWorkflowInstitution(onboarding, "INSTITUTION");
    }

    @Test
    void createContract() {

        UserResource userResource = createUserResource();
        UserResource delegateResource = createUserResource();

        Onboarding onboarding = createOnboarding();
        User manager = new User();
        manager.setId(userResource.getId().toString());
        manager.setRole(PartyRole.MANAGER);
        User delegate = new User();
        delegate.setId(delegateResource.getId().toString());
        delegate.setRole(PartyRole.DELEGATE);
        onboarding.setUsers(List.of(manager, delegate));

        Product product = createDummyProduct();

        when(userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST,manager.getId()))
                        .thenReturn(userResource);

        when(userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST,delegate.getId()))
                .thenReturn(delegateResource);

        when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(product);

        OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);
        onboardingService.createContract(onboardingWorkflow);

        Mockito.verify(userRegistryApi, Mockito.times(1))
                .findByIdUsingGET(USERS_WORKS_FIELD_LIST,manager.getId());

        Mockito.verify(userRegistryApi, Mockito.times(1))
                .findByIdUsingGET(USERS_WORKS_FIELD_LIST,delegate.getId());

        Mockito.verify(productService, Mockito.times(1))
                .getProductIsValid(onboarding.getProductId());

        ArgumentCaptor<String> captorTemplatePath = ArgumentCaptor.forClass(String.class);
        Mockito.verify(contractService, Mockito.times(1))
                .createContractPDF(captorTemplatePath.capture(), any(), any(), any(), any());
        assertEquals(captorTemplatePath.getValue(), product.getContractTemplatePath());
    }

    private Product createDummyProduct() {
        Product product = new Product();
        product.setContractTemplatePath("example");
        product.setContractTemplateVersion("version");
        product.setTitle("Title");
        product.setId(productId);
        return product;
    }
    @Test
    void saveToken_shouldSkipIfTokenExists() {
        Onboarding onboarding = createOnboarding();
        Token token = new Token();
        token.setId(UUID.randomUUID().toString());

        when(tokenRepository.findByOnboardingId(onboarding.getId()))
                .thenReturn(Optional.of(token));

        onboardingService.saveTokenWithContract(onboarding);

        Mockito.verify(tokenRepository, Mockito.times(1))
                .findByOnboardingId(onboarding.getId());
        Mockito.verifyNoMoreInteractions(tokenRepository);
    }


    @Test
    void saveToken() {
        Onboarding onboarding = createOnboarding();
        File contract = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("application.properties")).getFile());
        DSSDocument document = new FileDocument(contract);
        String digestExpected = document.getDigest(DigestAlgorithm.SHA256);

        Product productExpected = createDummyProduct();
        when(contractService.retrieveContractNotSigned(onboarding.getId(), productExpected.getTitle()))
                .thenReturn(contract);
        when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(productExpected);

        Mockito.doNothing().when(tokenRepository).persist(any(Token.class));

        onboardingService.saveTokenWithContract(onboarding);


        ArgumentCaptor<Token> tokenArgumentCaptor = ArgumentCaptor.forClass(Token.class);
        Mockito.verify(tokenRepository, Mockito.times(1))
                .persist(tokenArgumentCaptor.capture());
        assertEquals(onboarding.getProductId(), tokenArgumentCaptor.getValue().getProductId());
        assertEquals(digestExpected, tokenArgumentCaptor.getValue().getChecksum());
        assertEquals(productExpected.getContractTemplatePath(), tokenArgumentCaptor.getValue().getContractTemplate());
        assertEquals(productExpected.getContractTemplateVersion(), tokenArgumentCaptor.getValue().getContractVersion());
    }

    @Test
    void loadContract() {

        Onboarding onboarding = createOnboarding();
        Product product = createDummyProduct();

        when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(product);

        onboardingService.loadContract(onboarding);

        Mockito.verify(productService, Mockito.times(1))
                .getProductIsValid(onboarding.getProductId());
        Mockito.verify(contractService, Mockito.times(1))
                .loadContractPDF(product.getContractTemplatePath(), onboarding.getId(), product.getTitle());
    }


    @Test
    void sendMailRegistrationWithContract() {

        Onboarding onboarding = createOnboarding();
        Product product = createDummyProduct();
        UserResource userResource = createUserResource();
        Token token = new Token();
        token.setId(UUID.randomUUID().toString());

        when(tokenRepository.findByOnboardingId(onboarding.getId()))
                .thenReturn(Optional.of(token));
        when(productService.getProduct(onboarding.getProductId()))
                .thenReturn(product);

        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequestUid()))
                .thenReturn(userResource);

        OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);

        doNothing().when(notificationService)
                .sendMailRegistrationForContract(onboarding.getId(),
                        onboarding.getInstitution().getDigitalAddress(),
                        userResource.getName().getValue(), userResource.getFamilyName().getValue(),
                        product.getTitle(), "description",
                        "default");

        onboardingService.sendMailRegistrationForContract(onboardingWorkflow);

        Mockito.verify(notificationService, times(1))
                .sendMailRegistrationForContract(onboarding.getId(),
                        onboarding.getInstitution().getDigitalAddress(),
                        userResource.getName().getValue(), userResource.getFamilyName().getValue(),
                        product.getTitle(), "description",
                        "default");
    }

    @Test
    void sendMailRegistrationWithContractAggregator() {

        Onboarding onboarding = createOnboarding();
        Product product = createDummyProduct();
        UserResource userResource = createUserResource();
        Token token = new Token();
        token.setId(UUID.randomUUID().toString());

        when(tokenRepository.findByOnboardingId(onboarding.getId()))
                .thenReturn(Optional.of(token));
        when(productService.getProduct(onboarding.getProductId()))
                .thenReturn(product);

        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequestUid()))
                .thenReturn(userResource);
        doNothing().when(notificationService)
                .sendMailRegistrationForContractAggregator(onboarding.getId(),
                        onboarding.getInstitution().getDigitalAddress(),
                        userResource.getName().getValue(), userResource.getFamilyName().getValue(),
                        product.getTitle());

        onboardingService.sendMailRegistrationForContractAggregator(onboarding);

        Mockito.verify(notificationService, times(1))
                .sendMailRegistrationForContractAggregator(onboarding.getId(),
                        onboarding.getInstitution().getDigitalAddress(),
                        userResource.getName().getValue(), userResource.getFamilyName().getValue(),
                        product.getTitle());
    }


    @Test
    void sendMailRegistrationWithContractWhenApprove() {

        Onboarding onboarding = createOnboarding();
        Product product = createDummyProduct();
        Token token = new Token();
        token.setId(UUID.randomUUID().toString());

        when(tokenRepository.findByOnboardingId(onboarding.getId()))
                .thenReturn(Optional.of(token));
        when(productService.getProduct(onboarding.getProductId()))
                .thenReturn(product);

        OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);

        doNothing().when(notificationService)
                .sendMailRegistrationForContract(onboarding.getId(),
                        onboarding.getInstitution().getDigitalAddress(),
                        onboarding.getInstitution().getDescription(), "",
                        product.getTitle(), "description",
                        "default");

        onboardingService.sendMailRegistrationForContractWhenApprove(onboardingWorkflow);

        Mockito.verify(notificationService, times(1))
                .sendMailRegistrationForContract(onboarding.getId(),
                        onboarding.getInstitution().getDigitalAddress(),
                        onboarding.getInstitution().getDescription(), "",
                        product.getTitle(), "description",
                        "default");
    }


    @Test
    void sendMailRegistrationWithContract_throwExceptionWhenTokenIsNotPresent() {
        Onboarding onboarding = createOnboarding();
        OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);
        when(tokenRepository.findByOnboardingId(onboarding.getId()))
                .thenReturn(Optional.empty());
        assertThrows(GenericOnboardingException.class, () -> onboardingService.sendMailRegistrationForContract(onboardingWorkflow));
    }


    @Test
    void sendMailRegistration() {

        UserResource userResource = createUserResource();
        Product product = createDummyProduct();
        Onboarding onboarding = createOnboarding();

        when(productService.getProduct(onboarding.getProductId()))
                .thenReturn(product);
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequestUid()))
                .thenReturn(userResource);
        doNothing().when(notificationService).sendMailRegistration(onboarding.getInstitution().getDescription(),
                        onboarding.getInstitution().getDigitalAddress(),
                        userResource.getName().getValue(), userResource.getFamilyName().getValue(),
                        product.getTitle());

        onboardingService.sendMailRegistration(onboarding);

        Mockito.verify(notificationService, times(1))
                .sendMailRegistration(onboarding.getInstitution().getDescription(),
                    onboarding.getInstitution().getDigitalAddress(),
                    userResource.getName().getValue(), userResource.getFamilyName().getValue(),
                    product.getTitle());
    }

    @Test
    void sendMailRegistrationApprove() {

        Onboarding onboarding = createOnboarding();
        Product product = createDummyProduct();
        UserResource userResource = createUserResource();

        when(productService.getProduct(onboarding.getProductId()))
                .thenReturn(product);
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequestUid()))
                .thenReturn(userResource);

        doNothing().when(notificationService)
                .sendMailRegistrationApprove(any(), any(), any(),any(),any());

        onboardingService.sendMailRegistrationApprove(onboarding);

        Mockito.verify(notificationService, times(1))
                .sendMailRegistrationApprove(onboarding.getInstitution().getDescription(),
                        userResource.getName().getValue(),
                        userResource.getFamilyName().getValue(),
                        product.getTitle(),
                        onboarding.getId());
    }


    @Test
    void sendMailRegistrationApprove_throwExceptionWhenTokenIsNotPresent() {
        Onboarding onboarding = createOnboarding();
        when(tokenRepository.findByOnboardingId(onboarding.getId()))
                .thenReturn(Optional.empty());
        assertThrows(GenericOnboardingException.class, () -> onboardingService.sendMailRegistrationApprove(onboarding));
    }


    @Test
    void sendMailOnboardingApprove() {

        Onboarding onboarding = createOnboarding();
        Product product = createDummyProduct();
        UserResource userResource = createUserResource();

        when(productService.getProduct(onboarding.getProductId()))
                .thenReturn(product);
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequestUid()))
                .thenReturn(userResource);
        doNothing().when(notificationService).sendMailOnboardingApprove(any(), any(), any(),any(),any());

        onboardingService.sendMailOnboardingApprove(onboarding);


        Mockito.verify(notificationService, times(1))
                .sendMailOnboardingApprove(onboarding.getInstitution().getDescription(),
                        userResource.getName().getValue(),
                        userResource.getFamilyName().getValue(),
                        product.getTitle(),
                        onboarding.getId());
    }


    @Test
    void sendMailOnboardingApprove_throwExceptionWhenTokenIsNotPresent() {
        Onboarding onboarding = createOnboarding();
        when(tokenRepository.findByOnboardingId(onboarding.getId()))
                .thenReturn(Optional.empty());
        assertThrows(GenericOnboardingException.class, () -> onboardingService.sendMailOnboardingApprove(onboarding));
    }
}
