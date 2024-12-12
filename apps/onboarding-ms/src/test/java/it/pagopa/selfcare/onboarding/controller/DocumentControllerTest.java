package it.pagopa.selfcare.onboarding.controller;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
@TestHTTPEndpoint(DocumentController.class)
@QuarkusTestResource(MongoTestResource.class)
class DocumentControllerTest {

    @InjectMock
    AzureBlobClient blobClient;

    @Test
    @TestSecurity(user = "userJwt")
    void getFiles_ByPath_OK() {
        // given
        final String path = "/test/test";
        List<String> result = new ArrayList<>();

        // when
        when(blobClient.getFiles(anyString())).thenReturn(result);

        given()
            .when()
            .contentType(ContentType.JSON)
            .pathParam("path", path)
            .get("{path}")
            .then()
            .statusCode(200);

        // then
        Mockito.verify(blobClient, times(1)).getFiles(anyString());

    }

    @Test
    @TestSecurity(user = "userJwt")
    void getFiles_OK() {
        // given
        List<String> result = new ArrayList<>();

        // when
        when(blobClient.getFiles()).thenReturn(result);

        given()
            .when()
            .contentType(ContentType.JSON)
            .get()
            .then()
            .statusCode(200);

        // then
        Mockito.verify(blobClient, times(1)).getFiles();

    }

}