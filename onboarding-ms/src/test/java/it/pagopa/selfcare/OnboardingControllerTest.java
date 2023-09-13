package it.pagopa.selfcare;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.pagopa.selfcare.controller.request.InstitutionRequest;
import it.pagopa.selfcare.controller.request.OnboardingRequest;
import it.pagopa.selfcare.controller.request.User;
import it.pagopa.selfcare.util.InstitutionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class OnboardingControllerTest {

    final static OnboardingRequest body;
    final static InstitutionRequest institution;

    static {
        body = new OnboardingRequest();
        body.setProductId("productId");

        User user = new User();
        user.setId("is");
        body.setUsers(List.of(user));

        institution = new InstitutionRequest();
        institution.setInstitutionType(InstitutionType.PA);
        institution.setTaxCode("taxCode");
        body.setInstitution(institution);
    }

    @Test
    public void onboarding_shouldNotValidBody() {

        given()
          .when()
                .body(new OnboardingRequest())
                .contentType(ContentType.JSON)
                .post("/onboarding")
          .then()
             .statusCode(400);
    }


    @Test
    public void onboarding() {

        given()
                .when()
                .body(body)
                .contentType(ContentType.JSON)
                .post("/onboarding")
                .then()
                .statusCode(200)
                .body(is("Hello from RESTEasy Reactive"));
    }

}