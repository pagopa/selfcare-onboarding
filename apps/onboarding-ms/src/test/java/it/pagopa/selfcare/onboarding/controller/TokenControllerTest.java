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
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(TokenController.class)
@QuarkusTestResource(MongoTestResource.class)
public class TokenControllerTest {

    @InjectMock
    TokenService tokenService;

    @Test
    @TestSecurity(user = "userJwt")
    void getToken() {

        final String onboardingId = "onboardingId";
        Token token = new Token();
        token.setId(ObjectId.get());
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
}
