package it.pagopa.selfcare.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.mongodb.panache.common.PanacheUpdate;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.Billing;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.core_json.api.DelegationApi;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.*;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_FIELD_LIST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class CompletionServiceDefaultTest {

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
    org.openapi.quarkus.user_json.api.UserApi userControllerApi;
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
    @RestClient
    @InjectMock
    DelegationApi delegationApi;

    final String productId = "productId";
    private static final UserResource userResource;

    static {
        userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        Map<String, WorkContactResource> map = new HashMap<>();
        userResource.setWorkContacts(map);
    }

    @Nested
    @TestProfile(CompletionServiceDefaultTest.UserMSProfile.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class UserMSProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("onboarding-functions.persist-users.active", "true");
        }
    }

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

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);
    }

    @Test
    void createOrRetrieveInstitutionSuccess() {
        Onboarding onboarding = createOnboarding();
        Institution institution = new Institution();
        institution.setId("actual-id");
        institution.setTaxCode("123");
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId("actual-id");
        response.setInstitutions(List.of(institutionResponse));

        when(institutionApi.getInstitutionsUsingGET(institution.getTaxCode(), null, null, null))
                .thenReturn(response);

        InstitutionResponse serviceResponse = completionServiceDefault.createOrRetrieveInstitution(onboarding);

        assertNotNull(serviceResponse);
        assertEquals("actual-id", serviceResponse.getId());
    }

    @Test
    void createOrRetrieveInstitutionFailure() {
        Onboarding onboarding = createOnboarding();
        Institution institution = new Institution();
        institution.setId("actual-id");
        institution.setTaxCode("123");
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        response.setInstitutions(List.of(institutionResponse, institutionResponse));

        when(institutionApi.getInstitutionsUsingGET(institution.getTaxCode(), null, null, null))
                .thenReturn(response);

        assertThrows(GenericOnboardingException.class, () -> completionServiceDefault.createOrRetrieveInstitution(onboarding));
    }

    void mockOnboardingUpdateAndExecuteCreateInstitution(Onboarding onboarding){
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
    void persistUpadatedAt(){
        Onboarding onboarding = createOnboarding();

        PanacheUpdate panacheUpdateMock = mock(PanacheUpdate.class);
        when(panacheUpdateMock.where("_id", onboarding.getId()))
                .thenReturn(Long.valueOf(1));
        when(onboardingRepository.update("activatedAt.id = ?1 and updatedAt = ?2 ", any(), any()))
                .thenReturn(panacheUpdateMock);

        completionServiceDefault.persistActivatedAt(onboarding);

        verify(onboardingRepository, times(1))
                .update("activatedAt = ?1 and updatedAt = ?2 ", any(), any());
    }

    @Test
    void rejectOutdatedOnboardings(){

        Onboarding onboarding = createOnboarding();
        onboarding.getInstitution().setOriginId("originId");
        onboarding.getInstitution().setOrigin(Origin.IPA);

        PanacheUpdate panacheUpdateMock = mock(PanacheUpdate.class);
        when(panacheUpdateMock.where("productId = ?1 and institution.origin = ?2 and institution.originId = ?3 and _id != ?4 (status = PENDING or status = TOBEVALIDATED)",
                onboarding.getProductId(), onboarding.getInstitution().getOrigin(), onboarding.getInstitution().getOriginId(), onboarding.getId()))
                .thenReturn(Long.valueOf(1));
        when(onboardingRepository.update("status = ?1 and updatedAt = ?2 ", any(), any()))
                .thenReturn(panacheUpdateMock);

        completionServiceDefault.rejectOutdatedOnboardings(onboarding);

        verify(onboardingRepository, times(1))
                .update("status = ?1 and updatedAt = ?2 ", any(), any());
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreateSaAnac() {
        Onboarding onboarding = createOnboarding();

        Institution institutionSa = new Institution();
        institutionSa.setTaxCode("taxCode");
        institutionSa.setInstitutionType(InstitutionType.SA);
        institutionSa.setOrigin(Origin.ANAC);
        onboarding.setInstitution(institutionSa);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromAnacUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreateAsIvassWithTaxCode() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
        institution.setInstitutionType(InstitutionType.AS);
        institution.setOrigin(Origin.IVASS);
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromIvassUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreateAsIvassWithOrigin() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.AS);
        institution.setOrigin(Origin.IVASS);
        institution.setOriginId("originId");
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(null, null, Origin.IVASS.getValue(), "originId"))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromIvassUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);
    }
    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreatePgAde() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
        institution.setInstitutionType(InstitutionType.PG);
        institution.setOrigin(Origin.ADE);
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromInfocamereUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);
    }
    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreatePaAOO() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
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

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);

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
        institution.setTaxCode("taxCode");
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

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);

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

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);

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
        when(institutionApi.createInstitutionUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);

        ArgumentCaptor<InstitutionRequest> captor = ArgumentCaptor.forClass(InstitutionRequest.class);
        verify(institutionApi, times(1))
                .createInstitutionUsingPOST(captor.capture());
        assertEquals(institution.getTaxCode(), captor.getValue().getTaxCode());
    }



    void mockOnboardingUpdateWhenPersistOnboarding(Onboarding onboarding){
        PanacheUpdate panacheUpdateMock = mock(PanacheUpdate.class);
        when(panacheUpdateMock.where("_id", onboarding.getId()))
                .thenReturn(Long.valueOf(1));
        when(onboardingRepository.update("activatedAt = ?1 and updatedAt = ?2 ", any(), any()))
                .thenReturn(panacheUpdateMock);
    }

    @Test
    void persistOnboarding_emailIsEmpty() {
        Onboarding onboarding = createOnboarding();

        when(institutionApi.onboardingInstitutionUsingPOST(any(), any()))
                .thenReturn(new InstitutionResponse());
        Token token = new Token();
        token.setContractSigned("contract-signed-path");
        when(tokenRepository.findByOnboardingId(onboarding.getId()))
                .thenReturn(Optional.of(token));

        mockOnboardingUpdateWhenPersistOnboarding(onboarding);

        completionServiceDefault.persistOnboarding(onboarding);

        ArgumentCaptor<InstitutionOnboardingRequest> captor = ArgumentCaptor.forClass(InstitutionOnboardingRequest.class);
        verify(institutionApi, times(1))
                .onboardingInstitutionUsingPOST(any(), captor.capture());

        verify(tokenRepository, times(1))
                .findByOnboardingId(onboarding.getId());

        InstitutionOnboardingRequest actual = captor.getValue();
        assertEquals(productId, actual.getProductId());
    }

    @Test
    void persistOnboarding() {
        Onboarding onboarding = createOnboarding();
        onboarding.setActivatedAt(LocalDateTime.now());
        when(institutionApi.onboardingInstitutionUsingPOST(any(), any()))
                .thenReturn(new InstitutionResponse());
        Token token = new Token();
        token.setContractSigned("contract-signed-path");
        when(tokenRepository.findByOnboardingId(onboarding.getId()))
                .thenReturn(Optional.of(token));

        mockOnboardingUpdateWhenPersistOnboarding(onboarding);

        completionServiceDefault.persistOnboarding(onboarding);

        ArgumentCaptor<InstitutionOnboardingRequest> captor = ArgumentCaptor.forClass(InstitutionOnboardingRequest.class);
        verify(institutionApi, times(1))
                .onboardingInstitutionUsingPOST(any(), captor.capture());

        verify(tokenRepository, times(1))
                .findByOnboardingId(onboarding.getId());

        InstitutionOnboardingRequest actual = captor.getValue();
        assertEquals(onboarding.getProductId(), actual.getProductId());
        assertEquals(onboarding.getPricingPlan(), actual.getPricingPlan());
        assertEquals(token.getContractSigned(), actual.getContractPath());
        assertEquals(onboarding.getIsAggregator(), actual.getIsAggregator());
        assertEquals(actual.getActivatedAt().getDayOfYear(), onboarding.getActivatedAt().getDayOfYear());
    }

    @Test
    void sendCompletedEmail() {

        Product product = createDummyProduct();
        Onboarding onboarding = createOnboarding();
        OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowInstitution(onboarding, "INSTITUTION");
        User user = createDummyUser(onboarding);

        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        when(productService.getProduct(onboarding.getProductId()))
                .thenReturn(product);
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, user.getId()))
                .thenReturn(userResource);
        doNothing().when(notificationService).sendCompletedEmail(any(), any(), any(), any(), any());

        completionServiceDefault.sendCompletedEmail(onboardingWorkflow);

        Mockito.verify(notificationService, times(1))
                .sendCompletedEmail(any(), any(), any(), any(), any());
    }

    @Test
    void sendMailRejection() {

        Product product = createDummyProduct();
        Onboarding onboarding = createOnboarding();
        createDummyUser(onboarding);

        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        when(productService.getProduct(onboarding.getProductId()))
                .thenReturn(product);
        doNothing().when(notificationService).sendMailRejection(any(), any(), any());
        completionServiceDefault.sendMailRejection(context, onboarding);

        Mockito.verify(notificationService, times(1))
                .sendMailRejection(any(), any(), any());
    }

    @Test
    void sendCompletedEmailAggregate() {

        Onboarding onboarding = createOnboarding();
        Aggregator aggregator= new Aggregator();
        aggregator.setDescription("description");
        onboarding.setAggregator(aggregator);

        User user = createDummyUser(onboarding);

        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, user.getId()))
                .thenReturn(userResource);
        doNothing().when(notificationService).sendCompletedEmailAggregate(any(), any());

        completionServiceDefault.sendCompletedEmailAggregate(onboarding);

        Mockito.verify(notificationService, times(1))
                .sendCompletedEmailAggregate(any(), any());
    }

    @Test
    void persistUsers() {

        Onboarding onboarding = createOnboarding();
        createDummyUser(onboarding);
        onboarding.setDelegationId("delegationId");

        Response response = new ServerResponse(null, 200, null);
        when(userControllerApi.usersUserIdPost(any(), any(), any())).thenReturn(response);

        completionServiceDefault.persistUsers(onboarding);

        Mockito.verify(userControllerApi, times(1))
                .usersUserIdPost(any(), any(), any());
    }

    @Test
    void persistUsersWithException() {
        Onboarding onboarding = createOnboarding();
        createDummyUser(onboarding);

        Response response = new ServerResponse(null, 500, null);
        when(userControllerApi.usersUserIdPost(any(), any(), any())).thenReturn(response);

        assertThrows(RuntimeException.class, () -> completionServiceDefault.persistUsers(onboarding));

    }

    @Test
    void createDelegation(){
        Onboarding onboarding = createOnboarding();
        onboarding.getInstitution().setId("institution-id");
        onboarding.getInstitution().setDescription("institution-description");
        Aggregator aggregator = new Aggregator();
        aggregator.setDescription("aggregator-description");
        aggregator.setId("aggregator-id");
        onboarding.setAggregator(aggregator);

        DelegationResponse delegationResponse = new DelegationResponse();
        delegationResponse.setId("delegation-id");

        ArgumentCaptor<DelegationRequest> capture = ArgumentCaptor.forClass(DelegationRequest.class);
        when(delegationApi.createDelegationUsingPOST(capture.capture()))
                .thenReturn(delegationResponse);

        String delegationId = completionServiceDefault.createDelegation(onboarding);

        Assertions.assertEquals(onboarding.getInstitution().getId(), capture.getValue().getFrom());
        Assertions.assertEquals(onboarding.getInstitution().getDescription(), capture.getValue().getInstitutionFromName());
        Assertions.assertEquals(onboarding.getAggregator().getId(), capture.getValue().getTo());
        Assertions.assertEquals(onboarding.getAggregator().getDescription(), capture.getValue().getInstitutionToName());
        Assertions.assertEquals(onboarding.getProductId(), capture.getValue().getProductId());
        Assertions.assertEquals("EA", capture.getValue().getType().name());
        Assertions.assertEquals("delegation-id", delegationId);
        Mockito.verify(delegationApi, times(1))
                .createDelegationUsingPOST(capture.capture());
    }

    @Test
    void createDelegationWithNullAggregator(){
        Onboarding onboarding = createOnboarding();
        onboarding.getInstitution().setId("institution-id");
        onboarding.getInstitution().setDescription("institution-description");

        Assertions.assertThrows(GenericOnboardingException.class,
                () -> completionServiceDefault.createDelegation(onboarding),
                "Aggregator is null, impossible to create delegation");
        Mockito.verifyNoInteractions(delegationApi);
    }

    @Test
    void testCreateAggregateOnboardingRequest() {
        // Given
        OnboardingAggregateOrchestratorInput input = createSampleOnboardingInput();
        Onboarding onboardingToUpdate = createSampleOnboarding();

        // When
        String onboardingId = completionServiceDefault.createAggregateOnboardingRequest(input);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        assertNotEquals(onboardingToUpdate.getId(), onboardingId);
        assertEquals(input.getAggregate().getTaxCode(), onboardingToUpdate.getInstitution().getTaxCode());
    }

    public static OnboardingAggregateOrchestratorInput createSampleOnboardingInput() {
        OnboardingAggregateOrchestratorInput input = new OnboardingAggregateOrchestratorInput();

        input.setId("1");
        input.setProductId("productId");
        input.setTestEnvProductIds(Collections.singletonList("testEnvProductId"));
        input.setPricingPlan("pricingPlan");

        Billing billing = new Billing();
        input.setBilling(billing);

        input.setSignContract(true);
        input.setExpiringDate(LocalDateTime.MAX);
        input.setUserRequestUid("example-uid");
        input.setWorkflowInstanceId("workflowInstanceId");
        input.setCreatedAt(LocalDateTime.MAX);
        input.setUpdatedAt(LocalDateTime.MAX);
        input.setActivatedAt(LocalDateTime.MAX);
        input.setDeletedAt(null);
        input.setReasonForReject(null);

        Institution institution = new Institution();
        institution.setOrigin(Origin.IPA);
        institution.setInstitutionType(InstitutionType.PA);
        institution.setId("institutionId");
        institution.setDescription("description");
        institution.setTaxCode("taxCode");
        input.setInstitution(institution);

        Institution aggregate = new Institution();
        aggregate.setTaxCode("taxCodeAggregate");
        aggregate.setDescription("descriptionAggregate");
        input.setAggregate(aggregate);

        User user = new User();
        input.setUsers(Collections.singletonList(user));

        return input;
    }

    // Method to create sample Onboarding
    public static Onboarding createSampleOnboarding() {
        Onboarding onboarding = new Onboarding();

        onboarding.setId("1");
        onboarding.setProductId("productId");
        onboarding.setTestEnvProductIds(Collections.singletonList("testEnvProductId"));
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATE);
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        institution.setOrigin(Origin.IPA);
        institution.setDescription("descriptionAggregate");
        institution.setTaxCode("taxCodeAggregate");
        onboarding.setInstitution(institution);
        onboarding.setUsers(Collections.singletonList(new User()));
        onboarding.setPricingPlan("pricingPlan");
        onboarding.setBilling(new Billing());
        onboarding.setSignContract(true);
        onboarding.setExpiringDate(LocalDateTime.MAX);
        onboarding.setStatus(OnboardingStatus.PENDING);
        onboarding.setUserRequestUid("example-uid");
        onboarding.setWorkflowInstanceId("workflowInstanceId");
        onboarding.setCreatedAt(LocalDateTime.MAX);
        onboarding.setUpdatedAt(LocalDateTime.MAX);
        onboarding.setActivatedAt(LocalDateTime.MAX);
        onboarding.setDeletedAt(null);
        onboarding.setReasonForReject(null);

        Aggregator aggregator = new Aggregator();
        aggregator.setId("institutionId");
        aggregator.setDescription("description");
        aggregator.setTaxCode("taxCode");
        onboarding.setAggregator(aggregator);

        return onboarding;
    }

    @Test
    void sendTestEmail() {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());

        doNothing().when(notificationService).sendTestEmail(executionContext);

        completionServiceDefault.sendTestEmail(executionContext);

        Mockito.verify(notificationService, times(1))
                .sendTestEmail(executionContext);
    }
    @Test
    void checkExistsDelegationTrue() {
        OnboardingAggregateOrchestratorInput input = new OnboardingAggregateOrchestratorInput();
        Institution aggregate = new Institution();
        aggregate.setTaxCode("taxCode");
        input.setAggregate(aggregate);

        Institution aggregator = new Institution();
        aggregator.setId("aggregatorId");
        input.setInstitution(aggregator);

        DelegationWithPaginationResponse delegationWithPaginationResponse = new DelegationWithPaginationResponse();
        DelegationResponse delegation = new DelegationResponse();
        delegation.setStatus(DelegationResponse.StatusEnum.ACTIVE);
        delegationWithPaginationResponse.setDelegations(List.of(delegation));

        when(delegationApi.getDelegationsUsingGET1(null, input.getInstitution().getId(), null, null, aggregate.getTaxCode(), null, null, null))
                .thenReturn(delegationWithPaginationResponse);

        String result = completionServiceDefault.existsDelegation(input);

        assertTrue(Boolean.parseBoolean(result));
    }

    @Test
    void checkExistsDelegationFalse() {
        OnboardingAggregateOrchestratorInput input = new OnboardingAggregateOrchestratorInput();
        Institution aggregate = new Institution();
        aggregate.setTaxCode("taxCode");
        input.setAggregate(aggregate);

        Institution aggregator = new Institution();
        aggregator.setId("aggregatorId");
        input.setInstitution(aggregator);

        DelegationWithPaginationResponse delegationWithPaginationResponse = new DelegationWithPaginationResponse();
        delegationWithPaginationResponse.setDelegations(Collections.emptyList());
        when(delegationApi.getDelegationsUsingGET1(null, aggregator.getId(), null, null, aggregator.getTaxCode(), null, null, null))
                .thenReturn(delegationWithPaginationResponse);

        String result = completionServiceDefault.existsDelegation(input);

        assertFalse(Boolean.parseBoolean(result));
    }

    @Nested
    @TestProfile(CompletionServiceDefaultTest.ForceCreationProfile.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ForceCreationProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("onboarding-functions.force-institution-persist", "true");
        }
    }

    @Test
    void forceInstitutionCreationFlagTrue(){
        // given
        Onboarding onboarding = createOnboarding();

        Institution institutionSa = new Institution();
        institutionSa.setTaxCode("taxCode");
        institutionSa.setInstitutionType(InstitutionType.SA);
        institutionSa.setOrigin(Origin.ANAC);
        onboarding.setInstitution(institutionSa);

        InstitutionsResponse institutionsResponse = new InstitutionsResponse();
        institutionsResponse.setInstitutions(List.of(dummyInstitutionResponse()));

        when(institutionApi.getInstitutionsUsingGET(any(), any(), any(), any()))
                .thenReturn(institutionsResponse);

        PanacheUpdate panacheUpdateMock = mock(PanacheUpdate.class);
        when(onboardingRepository.update("institution.id = ?1 and updatedAt = ?2 ", any(), any()))
                .thenReturn(panacheUpdateMock);

        // when
        completionServiceDefault.createInstitutionAndPersistInstitutionId(onboarding);

        // then
        verify(institutionApi, times(1)).getInstitutionsUsingGET(any(), any(), any(), any());
    }

    private User createDummyUser(Onboarding onboarding) {
        User user = new User();
        user.setRole(PartyRole.MANAGER);
        user.setId("user-id");
        onboarding.setUsers(List.of(user));
        return user;
    }

    private InstitutionResponse dummyInstitutionResponse() {
        InstitutionResponse response = new InstitutionResponse();
        response.setId("response-id");
        return response;
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
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
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

}

