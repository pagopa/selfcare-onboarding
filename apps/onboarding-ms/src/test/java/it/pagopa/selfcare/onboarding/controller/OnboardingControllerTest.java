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
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @ValueSource(strings = {"/psp","/pa"})
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

        Mockito.when(onboardingService.onboarding(any()))
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

        /* PA */
        OnboardingPaRequest onboardingPaValid = new OnboardingPaRequest();
        onboardingPaValid.setProductId("productId");


        BillingPaRequest billingPaRequest = new BillingPaRequest();
        billingPaRequest.setRecipientCode("code");
        billingPaRequest.setVatNumber("vat");

        onboardingPaValid.setUsers(List.of(userDTO));
        onboardingPaValid.setInstitution(institution);
        onboardingPaValid.setBilling(billingPaRequest);

        Mockito.when(onboardingService.onboardingPa(any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPaValid)
                .contentType(ContentType.JSON)
                .post("/pa")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingPsp() {

        Mockito.when(onboardingService.onboardingPsp(any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPspValid)
                .contentType(ContentType.JSON)
                .post("/psp")
                .then()
                .statusCode(200);
    }

    //@Test
    void onboardingPg() {

        given()
                .when()
                .body(onboardingPgValid)
                .contentType(ContentType.JSON)
                .post("/pg")
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
    void complete() throws IOException {
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
    @TestSecurity(user = "userJwt")
    void deleteOK(){
        String onboardingId = "actual-onboarding-id";

        when(onboardingService.deleteOnboarding(onboardingId))
                .thenReturn(Uni.createFrom().item(1L));

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .put("/{onboardingId}/delete")
                .then()
                .statusCode(204);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .deleteOnboarding(expectedId.capture());
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void deleteInvalidOnboardingIdOrOnboardingNotFound(){
        String onboardingId = "actual-onboarding-id";

        when(onboardingService.deleteOnboarding(onboardingId))
                .thenThrow(InvalidRequestException.class);

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .put("/{onboardingId}/delete")
                .then()
                .statusCode(400);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .deleteOnboarding(expectedId.capture());
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getOnboarding(){
        OnboardingGetResponse response = getOnboardingGetResponse();
        when(onboardingService.onboardingGet("prod-io", "taxCode", "ACTIVE", "2023-12-01", "2023-12-31", 0, 20))
                .thenReturn(Uni.createFrom().item(response));

        Map<String, String> queryParameterMap = getStringStringMap();

        given()
                .when()
                .queryParams(queryParameterMap)
                .get()
                .then()
                .statusCode(200);

        verify(onboardingService, times(1))
                .onboardingGet("prod-io", "taxCode", "ACTIVE", "2023-12-01", "2023-12-31", 0, 20);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getOnboardingById(){
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
    void getOnboardingByIdWithUserInfo(){
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
    void getOnboardingPending(){
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
    private static Map<String, String> getStringStringMap() {
        Map<String, String> queryParameterMap = new HashMap<>();
        queryParameterMap.put("productId","prod-io");
        queryParameterMap.put("taxCode","taxCode");
        queryParameterMap.put("from","2023-12-01");
        queryParameterMap.put("to","2023-12-31");
        queryParameterMap.put("status","ACTIVE");
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
        onboarding.setInstitution(institutionResponse);
        return onboarding;
    }

}