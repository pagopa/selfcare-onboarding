package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mongodb.panache.common.PanacheUpdate;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.*;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;
import org.openapi.quarkus.user_json.api.UserControllerApi;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.time.LocalDateTime;
import java.util.*;

import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_FIELD_LIST;
import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_WORKS_FIELD_LIST;
import static org.junit.jupiter.api.Assertions.*;
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
    UserControllerApi userControllerApi;
    @RestClient
    @InjectMock
    UserApi userRegistryApi;
    @RestClient
    @InjectMock
    AooApi aooApi;
    @RestClient
    @InjectMock
    UoApi uoApi;
    @RestClient
    @InjectMock
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionRegistryProxyApi;

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
        when(panacheUpdateMock.where("_id", onboarding.getId()))
                .thenReturn(Long.valueOf(1));
        when(onboardingRepository.update("institution.id = ?1 and updatedAt = ?2 ", any(), any()))
                .thenReturn(panacheUpdateMock);

        completionServiceDefault.createInstitutionAndPersistInstitutionId(onboarding);

        verify(onboardingRepository, times(1))
                .update("institution.id = ?1 and updatedAt = ?2 ", any(), any());
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
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreatePaAOO() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        institution.setSubunitType(InstitutionPaSubunitType.AOO);
        institution.setSubunitCode("code");
        onboarding.setInstitution(institution);

        AOOResource aooResource = new AOOResource();
        when(aooApi.findByUnicodeUsingGET(institution.getSubunitCode(), null))
                .thenReturn(aooResource);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromIpaUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding, institutionResponse);

        ArgumentCaptor<InstitutionFromIpaPost> captor = ArgumentCaptor.forClass(InstitutionFromIpaPost.class);
        ArgumentCaptor<String> subunitCodeCaptor = ArgumentCaptor.forClass(String.class);
        verify(aooApi, times(1))
                .findByUnicodeUsingGET(subunitCodeCaptor.capture(), any());
        assertEquals(institution.getSubunitCode(), subunitCodeCaptor.getValue());
        verify(institutionApi, times(1))
                .createInstitutionFromIpaUsingPOST(captor.capture());
        assertEquals(institution.getTaxCode(), captor.getValue().getTaxCode());
        assertEquals(institution.getSubunitCode(), captor.getValue().getSubunitCode());
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreatePaUO() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        institution.setSubunitType(InstitutionPaSubunitType.UO);
        institution.setSubunitCode("code");
        onboarding.setInstitution(institution);

        UOResource uoResource = new UOResource();
        when(uoApi.findByUnicodeUsingGET1(institution.getSubunitCode(), institution.getInstitutionType().name()))
                .thenReturn(uoResource);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromIpaUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding, institutionResponse);

        ArgumentCaptor<InstitutionFromIpaPost> captor = ArgumentCaptor.forClass(InstitutionFromIpaPost.class);
        ArgumentCaptor<String> subunitCodeCaptor = ArgumentCaptor.forClass(String.class);
        verify(uoApi, times(1))
                .findByUnicodeUsingGET1(subunitCodeCaptor.capture(), any());
        assertEquals(institution.getSubunitCode(), subunitCodeCaptor.getValue());
        verify(institutionApi, times(1))
                .createInstitutionFromIpaUsingPOST(captor.capture());
        assertEquals(institution.getTaxCode(), captor.getValue().getTaxCode());
        assertEquals(institution.getSubunitCode(), captor.getValue().getSubunitCode());
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreatePa() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        institution.setTaxCode("taxCode");
        onboarding.setInstitution(institution);

        InstitutionResource institutionResource = new InstitutionResource();
        when(institutionRegistryProxyApi.findInstitutionUsingGET(institution.getTaxCode(), null, null))
                .thenReturn(institutionResource);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                null, null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromIpaUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding, institutionResponse);

        ArgumentCaptor<InstitutionFromIpaPost> captor = ArgumentCaptor.forClass(InstitutionFromIpaPost.class);
        ArgumentCaptor<String> taxCodeCaptor = ArgumentCaptor.forClass(String.class);
        verify(institutionRegistryProxyApi, times(1))
                .findInstitutionUsingGET(taxCodeCaptor.capture(), any(), any());
        assertEquals(institution.getTaxCode(), taxCodeCaptor.getValue());
        verify(institutionApi, times(1))
                .createInstitutionFromIpaUsingPOST(captor.capture());
        assertEquals(institution.getTaxCode(), captor.getValue().getTaxCode());
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreate() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.GSP);
        onboarding.setInstitution(institution);

        WebApplicationException e = new WebApplicationException(404);
        when(institutionRegistryProxyApi.findInstitutionUsingGET(institution.getTaxCode(), null ,null))
                .thenThrow(e);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                null, null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionUsingPOST1(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding, institutionResponse);

        ArgumentCaptor<InstitutionRequest> captor = ArgumentCaptor.forClass(InstitutionRequest.class);
        verify(institutionApi, times(1))
                .createInstitutionUsingPOST1(captor.capture());
        assertEquals(institution.getTaxCode(), captor.getValue().getTaxCode());
    }



    @Test
    void persistOnboarding_emailIsEmpty() {
        Onboarding onboarding = createOnboarding();

        User manager = new User();
        manager.setId("id");
        manager.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(manager));

        when(userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, manager.getId()))
                .thenReturn(new UserResource());

        when(institutionApi.onboardingInstitutionUsingPOST(any(), any()))
                .thenReturn(new InstitutionResponse());
        Token token = new Token();
        token.setContractSigned("contract-signed-path");
        when(tokenRepository.findByOnboardingId(onboarding.getId()))
                .thenReturn(Optional.of(token));

        completionServiceDefault.persistOnboarding(onboarding);

        ArgumentCaptor<InstitutionOnboardingRequest> captor = ArgumentCaptor.forClass(InstitutionOnboardingRequest.class);
        verify(institutionApi, times(1))
                .onboardingInstitutionUsingPOST(any(), captor.capture());

        verify(tokenRepository, times(1))
                .findByOnboardingId(onboarding.getId());

        InstitutionOnboardingRequest actual = captor.getValue();
        assertEquals(1, actual.getUsers().size());
        assertNull(actual.getUsers().get(0).getEmail());
    }

    @Test
    void persistOnboarding() {
        Onboarding onboarding = createOnboarding();

        User manager = new User();
        manager.setId("id");
        manager.setRole(PartyRole.MANAGER);
        manager.setUserMailUuid(UUID.randomUUID().toString());
        onboarding.setUsers(List.of(manager));
        onboarding.setActivatedAt(LocalDateTime.now());

        UserResource userResource = dummyUserResource(manager.getUserMailUuid());

        when(userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, manager.getId()))
                .thenReturn(userResource);
        when(institutionApi.onboardingInstitutionUsingPOST(any(), any()))
                .thenReturn(new InstitutionResponse());
        Token token = new Token();
        token.setContractSigned("contract-signed-path");
        when(tokenRepository.findByOnboardingId(onboarding.getId()))
                .thenReturn(Optional.of(token));

        completionServiceDefault.persistOnboarding(onboarding);

        ArgumentCaptor<InstitutionOnboardingRequest> captor = ArgumentCaptor.forClass(InstitutionOnboardingRequest.class);
        verify(institutionApi, times(1))
                .onboardingInstitutionUsingPOST(any(), captor.capture());

        verify(tokenRepository, times(1))
                .findByOnboardingId(onboarding.getId());

        InstitutionOnboardingRequest actual = captor.getValue();
        assertEquals(onboarding.getProductId(), actual.getProductId());
        assertEquals(onboarding.getPricingPlan(), actual.getPricingPlan());
        assertEquals(1, actual.getUsers().size());
        assertEquals(MANAGER_WORKCONTRACT_MAIL, actual.getUsers().get(0).getEmail());
        assertEquals(manager.getRole().name(), actual.getUsers().get(0).getRole().name());
        assertEquals(token.getContractSigned(), actual.getContractPath());
        assertEquals(actual.getActivatedAt().getDayOfYear(), onboarding.getActivatedAt().getDayOfYear());
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
        doNothing().when(notificationService).sendCompletedEmail(any(), any(), any(), any());

        completionServiceDefault.sendCompletedEmail(onboarding);

        Mockito.verify(notificationService, times(1))
                .sendCompletedEmail(any(), any(), any(), any());
    }

    @Test
    void sendMailRejection() {

        Product product = createDummyProduct();
        Onboarding onboarding = createOnboarding();

        User user = new User();
        user.setRole(PartyRole.MANAGER);
        user.setId("user-id");
        onboarding.setUsers(List.of(user));

        when(productService.getProduct(onboarding.getProductId()))
                .thenReturn(product);
        doNothing().when(notificationService).sendMailRejection(any(), any(), any());

        completionServiceDefault.sendMailRejection(onboarding);

        Mockito.verify(notificationService, times(1))
                .sendMailRejection(any(), any(), any());
    }

    @Test
    void persistUsers() {

        Onboarding onboarding = createOnboarding();

        User user = new User();
        user.setRole(PartyRole.MANAGER);
        user.setId("user-id");
        onboarding.setUsers(List.of(user));


        when(tokenRepository.findByOnboardingId(any()))
                .thenReturn(Optional.of(new Token()));

        Response response = new ServerResponse(null, 200, null);
        when(userControllerApi.usersUserIdPost(any(), any())).thenReturn(response);

        completionServiceDefault.persistUsers(onboarding);

        Mockito.verify(userControllerApi, times(1))
                .usersUserIdPost(any(), any());
    }

    private InstitutionResponse dummyInstitutionResponse() {
        InstitutionResponse response = new InstitutionResponse();
        response.setId("response-id");
        return  response;
    }


    private Onboarding createOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboarding.getId());
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

    private UserResource dummyUserResource(String userMailUuid){
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
        map.put(userMailUuid, workContactResource);
        userResource.setWorkContacts(map);
        return userResource;
    }
}

