package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestHTTPEndpoint(TokenController.class)
@QuarkusTestResource(MongoTestResource.class)
public class TokenControllerTest {

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
        given()
                .when()
                .pathParam("tokenId", "actual-token-id")
                .contentType(ContentType.MULTIPART)
                .multiPart("contract", testFile)
                .put("/{tokenId}/complete")
                .then()
                .statusCode(204);
    }
}
