package it.pagopa.selfcare;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.pagopa.selfcare.controller.request.*;
import it.pagopa.selfcare.util.InstitutionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class OnboardingControllerTest {

    final static OnboardingPspRequest onboardingPspValid;
    final static OnboardingPaRequest onboardingPaValid;
    final static OnboardingDefaultRequest onboardingBaseValid;
    final static InstitutionBaseRequest institution;
    final static InstitutionPspRequest institutionPsp;

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
    }

    @Test
    public void onboarding_shouldNotValidBody() {

        given()
          .when()
                .body(new OnboardingDefaultRequest())
                .contentType(ContentType.JSON)
                .post("/onboarding")
          .then()
             .statusCode(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/onboarding/psp","/onboarding/pa"})
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

        given()
                .when()
                .body(onboardingBaseValid)
                .contentType(ContentType.JSON)
                .post("/onboarding")
                .then()
                .statusCode(200);
    }

    @Test
    public void onboardingPa() {

        given()
                .when()
                .body(onboardingPaValid)
                .contentType(ContentType.JSON)
                .post("/onboarding/pa")
                .then()
                .statusCode(200);
    }

    @Test
    public void onboardingPsp() {

        given()
                .when()
                .body(onboardingPspValid)
                .contentType(ContentType.JSON)
                .post("/onboarding/psp")
                .then()
                .statusCode(200);
    }



    @Test
    public void onboardingPg() {

        OnboardingPgRequest request = new OnboardingPgRequest();
        request.setTaxCode("code");
        request.setProductId("productId");
        request.setDigitalAddress("email@pagopa.it");

        UserRequest userDTO = new UserRequest();
        userDTO.setId("is");
        request.setUsers(List.of(userDTO));

        given()
                .when()
                .body(request)
                .contentType(ContentType.JSON)
                .post("/onboarding/pg")
                .then()
                .statusCode(200);
    }

}