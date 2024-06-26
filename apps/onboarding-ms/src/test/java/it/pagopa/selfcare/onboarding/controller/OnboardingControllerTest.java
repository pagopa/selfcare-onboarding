package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestHTTPEndpoint(OnboardingController.class)
@QuarkusTestResource(MongoTestResource.class)
class OnboardingControllerTest {

    final static OnboardingPspRequest onboardingPspValid;
    final static UserRequest userDTO;
    final static OnboardingPgRequest onboardingPgValid;
    final static OnboardingDefaultRequest onboardingBaseValid;

    final static InstitutionBaseRequest institution;
    final static InstitutionPspRequest institutionPsp;

    @InjectMock
    OnboardingService onboardingService;

    static {
        onboardingBaseValid = new OnboardingDefaultRequest();
        onboardingBaseValid.setProductId("productId");

        userDTO = new UserRequest();
        userDTO.setTaxCode("taxCode");

        BillingRequest billingRequest = new BillingRequest();
        billingRequest.setVatNumber("vatNumber");

        onboardingBaseValid.setUsers(List.of(userDTO));
        onboardingBaseValid.setBilling(billingRequest);

        institution = new InstitutionBaseRequest();
        institution.setInstitutionType(InstitutionType.PT);
        institution.setTaxCode("taxCode");
        institution.setDigitalAddress("example@example.it");
        onboardingBaseValid.setInstitution(institution);

        /* PSP */
        onboardingPspValid = new OnboardingPspRequest();
        onboardingPspValid.setProductId("productId");
        onboardingPspValid.setUsers(List.of(userDTO));

        institutionPsp = new InstitutionPspRequest();
        institutionPsp.setInstitutionType(InstitutionType.PT);
        institutionPsp.setTaxCode("taxCode");
        institutionPsp.setDigitalAddress("example@example.it");
        institutionPsp.setPaymentServiceProvider(new PaymentServiceProviderRequest());
        onboardingPspValid.setInstitution(institutionPsp);

        /* PG */

        onboardingPgValid = new OnboardingPgRequest();
        onboardingPgValid.setTaxCode("code");
        onboardingPgValid.setProductId("productId");
        onboardingPgValid.setDigitalAddress("email@pagopa.it");
        onboardingPgValid.setUsers(List.of(userDTO));

    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboarding_shouldNotValidBody() {

        given()
                .when()
                .body(new OnboardingDefaultRequest())
                .contentType(ContentType.JSON)
                .post()
                .then()
                .statusCode(400);
    }

    @ParameterizedTest
    @TestSecurity(user = "userJwt")
    @ValueSource(strings = {"/psp", "/pa"})
    void onboarding_shouldNotValidPspBody(String path) {

        given()
                .when()
                .body(new OnboardingDefaultRequest())
                .contentType(ContentType.JSON)
                .post(path)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboarding() {

        Mockito.when(onboardingService.onboarding(any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingBaseValid)
                .contentType(ContentType.JSON)
                .post()
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingPa() {
        OnboardingPaRequest onboardingPaValid = dummyOnboardingPa();

        Mockito.when(onboardingService.onboarding(any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPaValid)
                .contentType(ContentType.JSON)
                .post("/pa")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1))
                .onboarding(captor.capture(), any());
        assertEquals(captor.getValue().getBilling().getRecipientCode(), onboardingPaValid.getBilling().getRecipientCode().toUpperCase());

    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingUsers() {
        OnboardingUserRequest onboardingUserRequest = dummyOnboardingUser();

        Mockito.when(onboardingService.onboardingUsers(any(), anyString()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingUserRequest)
                .contentType(ContentType.JSON)
                .post("/users")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingUsers_shouldNotValidBody() {

        given()
                .when()
                .body(new OnboardingUserRequest())
                .contentType(ContentType.JSON)
                .post("/users")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingPsp() {

        Mockito.when(onboardingService.onboarding(any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPspValid)
                .contentType(ContentType.JSON)
                .post("/psp")
                .then()
                .statusCode(200);
    }

    @Test
    void complete_unauthorized() {

        given()
                .when()
                .pathParam("tokenId", "actual-token-id")
                .contentType(ContentType.MULTIPART)
                .put("/{tokenId}/complete")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void complete() {
        File testFile = new File("src/test/resources/application.properties");
        String onboardingId = "actual-onboarding-id";

        when(onboardingService.complete(any(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .contentType(ContentType.MULTIPART)
                .multiPart("contract", testFile)
                .put("/{onboardingId}/complete")
                .then()
                .statusCode(204);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .complete(expectedId.capture(), any());
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    void completeOnboardingUsers_unauthorized() {

        given()
                .when()
                .pathParam("tokenId", "actual-token-id")
                .contentType(ContentType.MULTIPART)
                .put("/{tokenId}/completeOnboardingUsers")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void completeOnboardingUsers() {
        File testFile = new File("src/test/resources/application.properties");
        String onboardingId = "actual-onboarding-id";

        when(onboardingService.completeOnboardingUsers(any(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .contentType(ContentType.MULTIPART)
                .multiPart("contract", testFile)
                .put("/{onboardingId}/completeOnboardingUsers")
                .then()
                .statusCode(204);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .completeOnboardingUsers(expectedId.capture(), any());
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void consume() {
        File testFile = new File("src/test/resources/application.properties");
        String onboardingId = "actual-onboarding-id";

        when(onboardingService.completeWithoutSignatureVerification(any(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .contentType(ContentType.MULTIPART)
                .multiPart("contract", testFile)
                .put("/{onboardingId}/consume")
                .then()
                .statusCode(204);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .completeWithoutSignatureVerification(expectedId.capture(), any());
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void deleteOK() {
        String onboardingId = "actual-onboarding-id";
        ReasonRequest reasonRequest = new ReasonRequest();
        reasonRequest.setReasonForReject("string");

        when(onboardingService.rejectOnboarding(onboardingId, "string"))
                .thenReturn(Uni.createFrom().item(1L));

        given()
                .when()
                .body(reasonRequest)
                .contentType(ContentType.JSON)
                .pathParam("onboardingId", onboardingId)
                .put("/{onboardingId}/reject")
                .then()
                .statusCode(204);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .rejectOnboarding(expectedId.capture(), eq("string"));
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void deleteInvalidOnboardingIdOrOnboardingNotFound() {
        String onboardingId = "actual-onboarding-id";
        ReasonRequest reasonRequest = new ReasonRequest();
        reasonRequest.setReasonForReject("string");

        when(onboardingService.rejectOnboarding(onboardingId, "string"))
                .thenThrow(InvalidRequestException.class);

        given()
                .when()
                .body(reasonRequest)
                .contentType(ContentType.JSON)
                .pathParam("onboardingId", onboardingId)
                .put("/{onboardingId}/reject")
                .then()
                .statusCode(400);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .rejectOnboarding(expectedId.capture(), eq("string"));
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getOnboarding() {
        OnboardingGetResponse response = getOnboardingGetResponse();
        OnboardingGetFilters filters = OnboardingGetFilters.builder()
                .productId("prod-io")
                .taxCode("taxCode")
                .from("2023-12-01")
                .to("2023-12-31")
                .status("ACTIVE")
                .build();
        when(onboardingService.onboardingGet(filters))
                .thenReturn(Uni.createFrom().item(response));

        Map<String, String> queryParameterMap = getStringStringMap();

        given()
                .when()
                .queryParams(queryParameterMap)
                .get()
                .then()
                .statusCode(204);

        verify(onboardingService, times(1))
                .onboardingGet((OnboardingGetFilters) any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getOnboardingById() {
        OnboardingGet onboardingGet = dummyOnboardingGet();
        when(onboardingService.onboardingGet(onboardingGet.getId()))
                .thenReturn(Uni.createFrom().item(onboardingGet));

        given()
                .when()
                .get("/" + onboardingGet.getId())
                .then()
                .statusCode(200);

        verify(onboardingService, times(1))
                .onboardingGet(onboardingGet.getId());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getOnboardingByIdWithUserInfo() {
        OnboardingGet onboardingGet = dummyOnboardingGet();
        when(onboardingService.onboardingGetWithUserInfo(onboardingGet.getId()))
                .thenReturn(Uni.createFrom().item(onboardingGet));

        given()
                .when()
                .get("/" + onboardingGet.getId() + "/withUserInfo")
                .then()
                .statusCode(200);

        verify(onboardingService, times(1))
                .onboardingGetWithUserInfo(onboardingGet.getId());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getOnboardingPending() {
        OnboardingGet onboardingGet = dummyOnboardingGet();
        when(onboardingService.onboardingPending(onboardingGet.getId()))
                .thenReturn(Uni.createFrom().item(onboardingGet));

        given()
                .when()
                .get("/{onboardingId}/pending", onboardingGet.getId())
                .then()
                .statusCode(200);

        verify(onboardingService, times(1))
                .onboardingPending(onboardingGet.getId());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void approve() {
        OnboardingGet onboardingGet = dummyOnboardingGet();
        when(onboardingService.approve(onboardingGet.getId()))
                .thenReturn(Uni.createFrom().item(onboardingGet));

        given()
                .when()
                .put("/{onboardingId}/approve", onboardingGet.getId())
                .then()
                .statusCode(200);

        verify(onboardingService, times(1))
                .approve(onboardingGet.getId());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingComplete() {

        Mockito.when(onboardingService.onboardingCompletion(any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingBaseValid)
                .contentType(ContentType.JSON)
                .post("/completion")
                .then()
                .statusCode(200);

        Mockito.verify(onboardingService, times(1))
                .onboardingCompletion(any(), any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingCompletePa() {

        OnboardingPaRequest onboardingPaValid = dummyOnboardingPa();

        Mockito.when(onboardingService.onboardingCompletion(any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPaValid)
                .contentType(ContentType.JSON)
                .post("/pa/completion")
                .then()
                .statusCode(200);

        Mockito.verify(onboardingService, times(1))
                .onboardingCompletion(any(), any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingCompletePsp() {

        Mockito.when(onboardingService.onboardingCompletion(any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPspValid)
                .contentType(ContentType.JSON)
                .post("/psp/completion")
                .then()
                .statusCode(200);

        Mockito.verify(onboardingService, times(1))
                .onboardingCompletion(any(), any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingCompletePg() {

        OnboardingPgRequest onboardingPgRequest = new OnboardingPgRequest();
        onboardingPgRequest.setProductId("productId");
        onboardingPgRequest.setUsers(List.of(userDTO));
        onboardingPgRequest.setTaxCode("taxCode");
        onboardingPgRequest.setDigitalAddress("digital@address.it");
        onboardingPgRequest.setOrigin(Origin.INFOCAMERE);

        Mockito.when(onboardingService.onboardingCompletion(any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPgRequest)
                .contentType(ContentType.JSON)
                .post("/pg/completion")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1))
                .onboardingCompletion(captor.capture(), any());
        assertEquals(captor.getValue().getInstitution().getInstitutionType(), InstitutionType.PG);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingImport() {

        OnboardingImportRequest onboardingImportRequest = dummyOnboardingImport();

        Mockito.when(onboardingService.onboardingImport(any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingImportRequest)
                .contentType(ContentType.JSON)
                .post("/pa/import")
                .then()
                .statusCode(200);

        Mockito.verify(onboardingService, times(1))
                .onboardingImport(any(), any(), any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboarding_shouldNotValidImportBody() {

        given()
                .when()
                .body(new OnboardingDefaultRequest())
                .contentType(ContentType.JSON)
                .post("/pa/import")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getInstitutionOnboardings() {
        OnboardingResponse onboardingResponse = dummyOnboardingResponse();
        List<OnboardingResponse> onboardingResponses = new ArrayList<>();
        onboardingResponses.add(onboardingResponse);
        when(onboardingService.institutionOnboardings("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.PENDING))
                .thenReturn(Uni.createFrom().item(onboardingResponses));

        Map<String, String> queryParameterMap = getStringStringMapOnboardings();

        given()
                .when()
                .queryParams(queryParameterMap)
                .get("/institutionOnboardings")
                .then()
                .statusCode(200);

        verify(onboardingService, times(1))
                .institutionOnboardings("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.PENDING);
    }

    private static Map<String, String> getStringStringMap() {
        Map<String, String> queryParameterMap = new HashMap<>();
        queryParameterMap.put("productId", "prod-io");
        queryParameterMap.put("taxCode", "taxCode");
        queryParameterMap.put("from", "2023-12-01");
        queryParameterMap.put("to", "2023-12-31");
        queryParameterMap.put("status", "ACTIVE");
        return queryParameterMap;
    }

    private static Map<String, String> getStringStringMapOnboardings() {
        Map<String, String> queryParameterMap = new HashMap<>();
        queryParameterMap.put("taxCode", "taxCode");
        queryParameterMap.put("subunitCode", "subunitCode");
        queryParameterMap.put("origin", "origin");
        queryParameterMap.put("originId", "originId");
        queryParameterMap.put("status", "PENDING");
        return queryParameterMap;
    }

    private static OnboardingGetResponse getOnboardingGetResponse() {
        OnboardingGet onboarding = dummyOnboardingGet();
        OnboardingGetResponse response = new OnboardingGetResponse();
        response.setCount(1L);
        response.setItems(List.of(onboarding));
        return response;
    }

    private static OnboardingGet dummyOnboardingGet() {
        OnboardingGet onboarding = new OnboardingGet();
        onboarding.setId("id");
        onboarding.setStatus("ACTIVE");
        onboarding.setProductId("prod-io");
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setTaxCode("taxCode");
        institutionResponse.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setInstitution(institutionResponse);
        return onboarding;
    }

    private static OnboardingResponse dummyOnboardingResponse() {
        OnboardingResponse onboarding = new OnboardingResponse();
        onboarding.setId("id");
        onboarding.setStatus("PENDING");
        onboarding.setProductId("prod-io");
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setTaxCode("taxCode");
        institutionResponse.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setInstitution(institutionResponse);
        return onboarding;
    }

    private OnboardingUserRequest dummyOnboardingUser() {
        OnboardingUserRequest onboardingUserRequest = new OnboardingUserRequest();
        onboardingUserRequest.setProductId("productId");
        onboardingUserRequest.setTaxCode("taxCode");
        onboardingUserRequest.setOriginId("originId");
        onboardingUserRequest.setOrigin("origin");
        onboardingUserRequest.setUsers(List.of(userDTO));
        return onboardingUserRequest;
    }

    private OnboardingPaRequest dummyOnboardingPa() {
        OnboardingPaRequest onboardingPaValid = new OnboardingPaRequest();
        onboardingPaValid.setProductId("productId");

        BillingPaRequest billingPaRequest = new BillingPaRequest();
        billingPaRequest.setRecipientCode("code");
        billingPaRequest.setVatNumber("vat");

        onboardingPaValid.setUsers(List.of(userDTO));
        onboardingPaValid.setInstitution(institution);
        onboardingPaValid.setBilling(billingPaRequest);

        return onboardingPaValid;
    }

    private OnboardingImportRequest dummyOnboardingImport() {
        InstitutionImportRequest importInstitution = new InstitutionImportRequest();
        importInstitution.setTaxCode("taxCode");
        OnboardingImportRequest onboardingImportValid = new OnboardingImportRequest();
        onboardingImportValid.setProductId("productId");
        onboardingImportValid.setContractImported(new OnboardingImportContract());
        onboardingImportValid.setUsers(List.of(userDTO));
        onboardingImportValid.setInstitution(importInstitution);

        return onboardingImportValid;
    }

    @Test
    @TestSecurity(user = "userJwt")
    void updateOnboardingOK(){
        String onboardingId = "actual-onboarding-id";
        Onboarding onboardingUpdate = new Onboarding();
        onboardingUpdate.setId(onboardingId);
        Billing billing = new Billing();
        billing.setRecipientCode("X123");
        onboardingUpdate.setBilling(billing);

        when(onboardingService.updateOnboarding(onboardingId, onboardingUpdate))
                .thenReturn(Uni.createFrom().item(1L));

        Map<String, String> queryParameterMap = getStringStringMapOnboardingStatusUpdate();

        given()
                .when()
                .queryParams(queryParameterMap)
                .body(onboardingUpdate)
                .contentType(ContentType.JSON)
                .pathParam("onboardingId", onboardingId)
                .put("/{onboardingId}/update")
                .then()
                .statusCode(204);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1))
                .updateOnboarding(any(), captor.capture());
        assertEquals(captor.getValue().getBilling().getRecipientCode(), billing.getRecipientCode());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void updateOnboardingNotFound(){
        String onboardingId = "actual-onboarding-id";
        Onboarding onboardingUpdate = new Onboarding();

        when(onboardingService.updateOnboarding(onboardingId, onboardingUpdate))
                .thenThrow(InvalidRequestException.class);

        Map<String, String> queryParameterMap = getStringStringMapOnboardingStatusUpdate();

        given()
                .when()
                .queryParams(queryParameterMap)
                .body(onboardingUpdate)
                .contentType(ContentType.JSON)
                .pathParam("onboardingId", onboardingId)
                .put("/{onboardingId}/update")
                .then()
                .statusCode(204);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1))
                .updateOnboarding(any(), captor.capture());
        assertNotEquals(captor.getValue().getId(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void checkManager() {
        OnboardingUserRequest request = new OnboardingUserRequest();

        when(onboardingService.checkManager(any()))
                .thenReturn(Uni.createFrom().item(true));

        given()
                .when()
                .body(request)
                .contentType(ContentType.JSON)
                .post("/check-manager")
                .then()
                .statusCode(200);

        Mockito.verify(onboardingService, times(1))
                .checkManager(any());
    }

    private Map<String, String> getStringStringMapOnboardingStatusUpdate() {
        Map<String, String> queryParameterMap = new HashMap<>();
        queryParameterMap.put("status", "COMPLETED");
        return  queryParameterMap;
    }

}