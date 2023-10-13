package it.pagopa.selfcare.service;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.config.OnboardingFunctionConfig;
import it.pagopa.selfcare.entity.Onboarding;
import it.pagopa.selfcare.entity.Token;
import it.pagopa.selfcare.entity.User;
import it.pagopa.selfcare.exception.GenericOnboardingException;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.repository.OnboardingRepository;
import it.pagopa.selfcare.repository.TokenRepository;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.selfcare.service.OnboardingService.USERS_FIELD_LIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
public class OnboardingServiceTest {

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

    private Onboarding createOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(ObjectId.get());
        onboarding.setProductId("productId");
        onboarding.setUsers(List.of());
        return onboarding;
    }

    @Test
    void getOnboarding() {
        Onboarding onboarding = createOnboarding();
        Mockito.when(onboardingRepository.findById(onboarding.getId())).thenReturn(onboarding);

        Onboarding actual = onboardingService.getOnboarding(onboarding.getId().toHexString());
        assertEquals(onboarding.getId().toHexString(), actual.getId().toHexString());
    }

    @Test
    void createContract_shouldThrowIfManagerNotfound() {
        Onboarding onboarding = createOnboarding();
        assertThrows(GenericOnboardingException.class, () -> onboardingService.createContract(onboarding));
    }

    @Test
    void createContract() {

        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        UserResource delegateResource = new UserResource();
        delegateResource.setId(UUID.randomUUID());

        Onboarding onboarding = createOnboarding();
        User manager = new User();
        manager.setId(userResource.getId().toString());
        manager.setRole(PartyRole.MANAGER);
        User delegate = new User();
        delegate.setId(delegateResource.getId().toString());
        delegate.setRole(PartyRole.DELEGATE);
        onboarding.setUsers(List.of(manager, delegate));

        Mockito.when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST,manager.getId()))
                        .thenReturn(userResource);

        Mockito.when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST,delegate.getId()))
                .thenReturn(delegateResource);

        Mockito.when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(new Product());

        onboardingService.createContract(onboarding);

        Mockito.verify(userRegistryApi, Mockito.times(1))
                .findByIdUsingGET(USERS_FIELD_LIST,manager.getId());

        Mockito.verify(userRegistryApi, Mockito.times(1))
                .findByIdUsingGET(USERS_FIELD_LIST,delegate.getId());

        Mockito.verify(productService, Mockito.times(1))
                .getProductIsValid(onboarding.getProductId());
    }

    private Product createDummyProduct() {
        Product product = new Product();
        product.setContractTemplatePath("example");
        product.setContractTemplateVersion("version");
        return product;
    }


    @Test
    void saveToken() {
        Onboarding onboarding = createOnboarding();
        File contract = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("application.properties")).getFile());
        DSSDocument document = new FileDocument(contract);
        String digestExpected = document.getDigest(DigestAlgorithm.SHA256);

        Mockito.when(contractService.retrieveContractNotSigned(onboarding.getId().toHexString()))
                        .thenReturn(contract);
        Product productExpected = createDummyProduct();
        Mockito.when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(productExpected);

        Mockito.doNothing().when(tokenRepository).persist(any(Token.class));

        onboardingService.saveToken(onboarding);


        ArgumentCaptor<Token> tokenArgumentCaptor = ArgumentCaptor.forClass(Token.class);
        Mockito.verify(tokenRepository, Mockito.times(1))
                .persist(tokenArgumentCaptor.capture());
        assertEquals(onboarding.getProductId(), tokenArgumentCaptor.getValue().getProductId());
        assertEquals(digestExpected, tokenArgumentCaptor.getValue().getChecksum());
        assertEquals(productExpected.getContractTemplatePath(), tokenArgumentCaptor.getValue().getContractTemplate());
        assertEquals(productExpected.getContractTemplateVersion(), tokenArgumentCaptor.getValue().getContractVersion());
    }
}
