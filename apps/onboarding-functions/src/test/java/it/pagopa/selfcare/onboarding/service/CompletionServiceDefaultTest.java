package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mongodb.panache.common.PanacheUpdate;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionFromIpaPost;
import org.openapi.quarkus.core_json.model.InstitutionOnboardingRequest;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.core_json.model.InstitutionsResponse;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.util.*;

import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_WORKS_FIELD_LIST;
import static it.pagopa.selfcare.onboarding.utils.PdfMapper.workContactsKey;
import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_FIELD_LIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@QuarkusTest
public class CompletionServiceDefaultTest {

    public static final String MANAGER_WORKCONTRACT_MAIL = "mail@mail.it";
    @Inject
    CompletionServiceDefault completionServiceDefault;

    @InjectMock
    OnboardingRepository onboardingRepository;
    @InjectMock
    TokenRepository tokenRepository;
    @InjectMock
    NotificationService notificationService;
    @InjectMock
    ProductService productService;

    @RestClient
    @InjectMock
    InstitutionApi institutionApi;
    @RestClient
    @InjectMock
    UserApi userRegistryApi;

    final String productId = "productId";

    @Test
    void createInstitutionAndPersistInstitutionId_shouldThrowExceptionIfMoreInstitutions() {
        Onboarding onboarding = createOnboarding();

        InstitutionsResponse response = new InstitutionsResponse();
        response.setInstitutions(List.of(new InstitutionResponse(), new InstitutionResponse()));
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null))
            .thenReturn(response);

        assertThrows(GenericOnboardingException.class, () -> completionServiceDefault.createInstitutionAndPersistInstitutionId(onboarding));
    }

    @Test
    void createInstitutionAndPersistInstitutionId_foundInstitution() {
        Onboarding onboarding = createOnboarding();

        InstitutionsResponse response = new InstitutionsResponse();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId("actual-id");
        response.setInstitutions(List.of(institutionResponse));
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null))
                .thenReturn(response);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding, institutionResponse);
    }

    void mockOnboardingUpdateAndExecuteCreateInstitution(Onboarding onboarding, InstitutionResponse institutionResponse){
        PanacheUpdate panacheUpdateMock = mock(PanacheUpdate.class);
        when(panacheUpdateMock.where("_id", onboarding.getOnboardingId()))
                .thenReturn(Long.valueOf(1));
        when(onboardingRepository.update("institution.id", institutionResponse.getId()))
                .thenReturn(panacheUpdateMock);

        completionServiceDefault.createInstitutionAndPersistInstitutionId(onboarding);

        verify(onboardingRepository, times(1))
                .update("institution.id", institutionResponse.getId());
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreateSaAnac() {
        Onboarding onboarding = createOnboarding();

        Institution institutionSa = new Institution();
        institutionSa.setInstitutionType(InstitutionType.SA);
        institutionSa.setOrigin(Origin.ANAC);
        onboarding.setInstitution(institutionSa);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromAnacUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding, institutionResponse);
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreateAsIvass() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.AS);
        institution.setOrigin(Origin.IVASS);
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromIvassUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding, institutionResponse);
    }
    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreatePgAde() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PG);
        institution.setOrigin(Origin.ADE);
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromInfocamereUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding, institutionResponse);
    }
    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreatePa() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        institution.setSubunitType(InstitutionPaSubunitType.AOO);
        institution.setSubunitCode("code");
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromIpaUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding, institutionResponse);

        ArgumentCaptor<InstitutionFromIpaPost> captor = ArgumentCaptor.forClass(InstitutionFromIpaPost.class);
        verify(institutionApi, times(1))
                .createInstitutionFromIpaUsingPOST(captor.capture());
        assertEquals(institution.getTaxCode(), captor.getValue().getTaxCode());
        assertEquals(institution.getSubunitCode(), captor.getValue().getSubunitCode());
    }

    @Test
    void persistOnboarding_workContractsNotFound() {
        Onboarding onboarding = createOnboarding();

        User manager = new User();
        manager.setId("id");
        manager.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(manager));

        when(userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, manager.getId()))
                .thenReturn(new UserResource());

        assertThrows(GenericOnboardingException.class, () -> completionServiceDefault.persistOnboarding(onboarding));
    }

    @Test
    void persistOnboarding() {
        Onboarding onboarding = createOnboarding();

        User manager = new User();
        manager.setId("id");
        manager.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(manager));

        UserResource userResource = dummyUserResource(onboarding.getOnboardingId());

        when(userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, manager.getId()))
                .thenReturn(userResource);
        when(institutionApi.onboardingInstitutionUsingPOST(any(), any()))
                .thenReturn(new InstitutionResponse());
        Token token = new Token();
        token.setContractSigned("contract-signed-path");
        when(tokenRepository.findByOnboardingId(onboarding.getOnboardingId()))
                .thenReturn(Optional.of(token));

        completionServiceDefault.persistOnboarding(onboarding);

        ArgumentCaptor<InstitutionOnboardingRequest> captor = ArgumentCaptor.forClass(InstitutionOnboardingRequest.class);
        verify(institutionApi, times(1))
                .onboardingInstitutionUsingPOST(any(), captor.capture());

        verify(tokenRepository, times(1))
                .findByOnboardingId(onboarding.getOnboardingId());

        InstitutionOnboardingRequest actual = captor.getValue();
        assertEquals(onboarding.getProductId(), actual.getProductId());
        assertEquals(onboarding.getPricingPlan(), actual.getPricingPlan());
        assertEquals(1, actual.getUsers().size());
        assertEquals(MANAGER_WORKCONTRACT_MAIL, actual.getUsers().get(0).getEmail());
        assertEquals(manager.getRole().name(), actual.getUsers().get(0).getRole().name());
        assertEquals(token.getContractSigned(), actual.getContractPath());
    }

    @Test
    void sendCompletedEmail() {

        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        Map<String, WorkContactResource> map = new HashMap<>();
        userResource.setWorkContacts(map);
        Product product = createDummyProduct();
        Onboarding onboarding = createOnboarding();

        User user = new User();
        user.setRole(PartyRole.MANAGER);
        user.setId("user-id");
        onboarding.setUsers(List.of(user));

        when(productService.getProduct(onboarding.getProductId()))
                .thenReturn(product);
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, user.getId()))
                .thenReturn(userResource);
        doNothing().when(notificationService).sendCompletedEmail(any(), any());

        completionServiceDefault.sendCompletedEmail(onboarding);

        Mockito.verify(notificationService, times(1))
                .sendCompletedEmail(any(), any());
    }

    private InstitutionResponse dummyInstitutionResponse() {
        InstitutionResponse response = new InstitutionResponse();
        response.setId("response-id");
        return  response;
    }


    private Onboarding createOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(ObjectId.get());
        onboarding.setOnboardingId(onboarding.getId().toHexString());
        onboarding.setProductId(productId);
        onboarding.setPricingPlan("pricingPlan");
        onboarding.setUsers(List.of());
        onboarding.setInstitution(new Institution());
        onboarding.setUserRequestUid("example-uid");

        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("example");
        billing.setVatNumber("example");
        onboarding.setBilling(billing);
        return onboarding;
    }

    private Product createDummyProduct() {
        Product product = new Product();
        product.setContractTemplatePath("example");
        product.setContractTemplateVersion("version");
        product.setTitle("Title");
        product.setId(productId);
        return product;
    }

    private UserResource dummyUserResource(String onboardingId){
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


        CertifiableFieldResourceOfstring resourceOfMail = new CertifiableFieldResourceOfstring();
        resourceOfMail.setCertification(CertifiableFieldResourceOfstring.CertificationEnum.NONE);
        resourceOfMail.setValue(MANAGER_WORKCONTRACT_MAIL);
        WorkContactResource workContactResource = new WorkContactResource();
        workContactResource.email(resourceOfMail);

        Map<String, WorkContactResource> map = new HashMap<>();
        map.put(workContactsKey.apply(onboardingId), workContactResource);
        userResource.setWorkContacts(map);
        return userResource;
    }
}

