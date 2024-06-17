package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import it.pagopa.selfcare.onboarding.service.NotificationService;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@QuarkusTest
@TestHTTPEndpoint(NotificationController.class)
@QuarkusTestResource(MongoTestResource.class)
class NotificationControllerTest {
    @InjectMock
    NotificationService notificationService;

    @Test
    @TestSecurity(user = "userJwt")
    void resendOnboardingNotifications_succeeds() {
        OnboardingGetFilters filters = OnboardingGetFilters.builder()
                .status("COMPLETED").build();

        given()
            .when()
            .body(filters)
            .contentType(ContentType.JSON).post("/resend")
            .then()
            .statusCode(200);

        verify(notificationService).resendOnboardingNotifications(any());
    }
}