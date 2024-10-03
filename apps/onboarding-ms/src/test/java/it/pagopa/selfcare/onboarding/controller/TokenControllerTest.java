package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.service.TokenService;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestHTTPEndpoint(TokenController.class)
@QuarkusTestResource(MongoTestResource.class)
class TokenControllerTest {

    @InjectMock
    private TokenService tokenService;

    @Test
    @TestSecurity(user = "userJwt")
    void getToken() {

        final String onboardingId = "onboardingId";
        Token token = new Token();
        token.setId(UUID.randomUUID().toString());
        when(tokenService.getToken(onboardingId))
                .thenReturn(Uni.createFrom().item(List.of(token)));

        given()
                .when()
                .contentType(ContentType.JSON)
                .queryParams("onboardingId", onboardingId)
                .get()
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getContract() {
        final String onboardingId = "onboardingId";
        RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok();
        when(tokenService.retrieveContractNotSigned(onboardingId))
                .thenReturn(Uni.createFrom().item(response.build()));

        given()
                .when()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .get("/{onboardingId}/contract", onboardingId)
                .then()
                .statusCode(200);


    }
}
