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
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
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
public class OnboardingControllerTest {

    final static OnboardingPspRequest onboardingPspValid;
    final static OnboardingPaRequest onboardingPaValid;
    final static OnboardingPgRequest onboardingPgValid;
    final static OnboardingDefaultRequest onboardingBaseValid;


    final static OnboardingSaRequest onboardingSaValid;

    final static InstitutionBaseRequest institution;
    final static InstitutionPspRequest institutionPsp;

    @InjectMock
    OnboardingService onboardingService;

    static {
        onboardingBaseValid = new OnboardingDefaultRequest();
        onboardingBaseValid.setProductId("productId");

        UserRequest userDTO = new UserRequest();
        userDTO.setTaxCode("taxCode");

        BillingRequest billingRequest = new BillingRequest();
        billingRequest.setRecipientCode("code");
        billingRequest.setVatNumber("vat");

        onboardingBaseValid.setUsers(List.of(userDTO));
        onboardingBaseValid.setBilling(billingRequest);

        institution = new InstitutionBaseRequest();
        institution.setInstitutionType(InstitutionType.PT);
        institution.setTaxCode("taxCode");
        onboardingBaseValid.setInstitution(institution);

        /* PA */
        onboardingPaValid = new OnboardingPaRequest();
        onboardingPaValid.setProductId("productId");

        onboardingPaValid.setUsers(List.of(userDTO));
        onboardingPaValid.setInstitution(institution);
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

        /* SA */

        onboardingSaValid = new OnboardingSaRequest();
        BillingSaRequest billingSaRequest = new BillingSaRequest();
        billingSaRequest.setVatNumber("vat");
        onboardingSaValid.setBilling(billingSaRequest);
        InstitutionBaseRequest institutionSa = new InstitutionBaseRequest();
        institutionSa.setInstitutionType(InstitutionType.SA);
        institutionSa.setTaxCode("taxCode");
        onboardingSaValid.setInstitution(institutionSa);
        onboardingSaValid.setUsers(List.of(userDTO));
        onboardingSaValid.setProductId("productId");
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
    @ValueSource(strings = {"/psp","/pa", "/sa"})
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

    @Test
    @TestSecurity(user = "userJwt")
    public void onboardingSa() {

        Mockito.when(onboardingService.onboardingSa(any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingSaValid)
                .contentType(ContentType.JSON)
                .post("/sa")
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