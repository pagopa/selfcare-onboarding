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
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestHTTPEndpoint(OnboardingController.class)
@QuarkusTestResource(MongoTestResource.class)
public class OnboardingControllerTest {

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
    public void onboarding_shouldNotValidBody() {

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
    public void onboarding_shouldNotValidPspBody(String path) {

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
    public void onboarding() {

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
    public void onboardingPa() {

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
    public void onboardingPsp() {

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
    public void onboardingPg() {

        given()
                .when()
                .body(onboardingPgValid)
                .contentType(ContentType.JSON)
                .post("/pg")
                .then()
                .statusCode(200);
    }


    @Test
    public void complete_unauthorized() {

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
    public void complete() throws IOException {
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
    void getOnboarding(){

        OnboardingGet onboarding = new OnboardingGet();
        onboarding.setId("id");
        OnboardingGetResponse response = new OnboardingGetResponse();
        response.setCount(1L);
        response.setItems(List.of(onboarding));
        when(onboardingService.onboardingGet(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .when()
                .get()
                .then()
                .statusCode(200);

        verify(onboardingService, times(1))
                .onboardingGet(any(), any(), any(), any(), any(), any(), any());
    }

}