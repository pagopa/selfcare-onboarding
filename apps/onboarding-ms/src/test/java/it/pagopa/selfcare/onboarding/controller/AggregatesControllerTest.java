package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.model.VerifyAggregateAppIoResponse;
import it.pagopa.selfcare.onboarding.model.VerifyAggregateResponse;
import it.pagopa.selfcare.onboarding.model.VerifyAggregateSendResponse;
import it.pagopa.selfcare.onboarding.service.AggregatesService;
import org.junit.jupiter.api.Test;

import java.io.File;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestHTTPEndpoint(AggregatesController.class)
@QuarkusTestResource(MongoTestResource.class)
@TestSecurity(user = "userJwt")
public class AggregatesControllerTest {

    @InjectMock
    AggregatesService aggregatesService;

    @Test
    void verifyAggregatesCsv_succeeds() {
        File testFile = new File("src/test/resources/aggregates-appio.csv");

        when(aggregatesService.validateAppIoAggregatesCsv(any()))
                .thenReturn(Uni.createFrom().item(new VerifyAggregateAppIoResponse()));

        given()
                .when()
                .contentType(ContentType.MULTIPART)
                .multiPart("aggregates", testFile)
                .post("/verification/prod-io")
                .then()
                .statusCode(200);

        verify(aggregatesService, times(1))
                .validateAppIoAggregatesCsv(any());
    }

    @Test
    void verifyAggregatesPagoPaCsv_succeeds() {
        File testFile = new File("src/test/resources/aggregates-pagopa.csv");

        when(aggregatesService.validatePagoPaAggregatesCsv(any()))
                .thenReturn(Uni.createFrom().item(new VerifyAggregateResponse()));

        given()
                .when()
                .contentType(ContentType.MULTIPART)
                .multiPart("aggregates", testFile)
                .post("/verification/prod-pagopa")
                .then()
                .statusCode(200);

        verify(aggregatesService, times(1))
                .validatePagoPaAggregatesCsv(any());
    }

    @TestSecurity(user = "userJwt")
    @Test
    void verifyAggregatesSendCsv_succeeds() {
        File testFile = new File("src/test/resources/aggregates-send.csv");

        when(aggregatesService.validateSendAggregatesCsv(any()))
                .thenReturn(Uni.createFrom().item(new VerifyAggregateSendResponse()));

        given()
                .when()
                .contentType(ContentType.MULTIPART)
                .multiPart("aggregates", testFile)
                .post("/verification/prod-pn")
                .then()
                .statusCode(200);

        verify(aggregatesService, times(1))
                .validateSendAggregatesCsv(any());
    }
}
