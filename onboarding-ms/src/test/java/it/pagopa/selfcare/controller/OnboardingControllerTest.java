package it.pagopa.selfcare.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.controller.request.*;
import it.pagopa.selfcare.controller.response.OnboardingResponse;
import it.pagopa.selfcare.service.OnboardingService;
import it.pagopa.selfcare.util.InstitutionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
@TestHTTPEndpoint(OnboardingController.class)
@QuarkusTestResource(MongoTestResource.class)
@TestSecurity(authorizationEnabled = false)
public class OnboardingControllerTest {

    final static OnboardingPspRequest onboardingPspValid;
    final static OnboardingPaRequest onboardingPaValid;
    final static OnboardingPgRequest onboardingPgValid;
    final static OnboardingDefaultRequest onboardingBaseValid;

    final static InstitutionBaseRequest institution;
    final static InstitutionPspRequest institutionPsp;

    @InjectMock
    OnboardingService onboardingService;

    static {
        onboardingBaseValid = new OnboardingDefaultRequest();
        onboardingBaseValid.setProductId("productId");

        UserRequest userDTO = new UserRequest();
        userDTO.setId("is");
        onboardingBaseValid.setUsers(List.of(userDTO));

        institution = new InstitutionBaseRequest();
        institution.setInstitutionType(InstitutionType.PT);
        institution.setTaxCode("taxCode");
        onboardingBaseValid.setInstitution(institution);

        /* PA */
        onboardingPaValid = new OnboardingPaRequest();
        onboardingPaValid.setProductId("productId");

        onboardingPaValid.setUsers(List.of(userDTO));
        onboardingPaValid.setInstitution(institution);
        BillingRequest billingRequest = new BillingRequest();
        billingRequest.setRecipientCode("code");
        billingRequest.setVatNumber("vat");
        onboardingPaValid.setBilling(billingRequest);

        /* PSP */
        onboardingPspValid = new OnboardingPspRequest();
        onboardingPspValid.setProductId("productId");
        onboardingPspValid.setUsers(List.of(userDTO));

        institutionPsp = new InstitutionPspRequest();
        institutionPsp.setInstitutionType(InstitutionType.PT);
        institutionPsp.setTaxCode("taxCode");
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
    @ValueSource(strings = {"/psp","/pa"})
    public void onboarding_shouldNotValidPspBody(String path) {

        given()
                .when()
                .body(onboardingBaseValid)
                .contentType(ContentType.JSON)
                .post(path)
                .then()
                .statusCode(400);
    }

    @Test
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
    public void onboardingPa() {

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

}