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
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.response.ContractSignedReport;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.service.TokenService;
import jakarta.ws.rs.core.MediaType;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
    when(tokenService.retrieveContract(onboardingId, false))
      .thenReturn(Uni.createFrom().item(response.build()));

    given()
      .when()
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .get("/{onboardingId}/contract", onboardingId)
      .then()
      .statusCode(200);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getContractSignedTest() {
    // given
    final String onboardingId = "onboardingId";
    RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok();

    when(tokenService.retrieveSignedFile(onboardingId))
      .thenReturn(Uni.createFrom().item(response.build()));

    // when
    given()
      .when()
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .get("/{onboardingId}/contract-signed", onboardingId)
      .then()
      .statusCode(200);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getAttachment() {
    final String onboardingId = "onboardingId";
    final String filename = "filename";
    RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok();
    when(tokenService.retrieveAttachment(onboardingId, filename))
      .thenReturn(Uni.createFrom().item(response.build()));

    given()
      .when()
      .queryParam("name", filename)
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .get("/{onboardingId}/attachment", onboardingId)
      .then()
      .statusCode(200);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getAttachmentBadRequest() {
    final String onboardingId = "onboardingId";
    given()
      .when()
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .get("/{onboardingId}/attachment", onboardingId)
      .then()
      .statusCode(400);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void updateContractSignedTest_OK() {
    // given
    final String onboardingId = "onboardingId";
    final String contractSigned = "contractSigned";

    Uni<Long> response = Uni.createFrom().item(Long.valueOf(1));
    when(tokenService.updateContractSigned(onboardingId, contractSigned)).thenReturn(response);

    // when
    given()
      .when()
      .queryParam("onboardingId", onboardingId)
      .queryParam("contractSigned", contractSigned)
      .contentType(MediaType.APPLICATION_JSON)
      .put("/contract-signed")
      .then()
      .statusCode(200);

    // then
    Mockito.verify(tokenService, times(1)).updateContractSigned(anyString(), anyString());
  }

  @Test
  @TestSecurity(user = "userJwt")
  void updateContractSignedTest_KO() {
    // given
    final String onboardingId = "onboardingId";
    final String contractSigned = "contractSigned";

    Uni<Long> response = Uni.createFrom()
      .failure(
        new InvalidRequestException(
          String.format("Error %S", onboardingId)));

    when(tokenService.updateContractSigned(onboardingId, contractSigned)).thenReturn(response);

    // when
    given()
      .when()
      .queryParam("onboardingId", onboardingId)
      .queryParam("contractSigned", contractSigned)
      .contentType(MediaType.APPLICATION_JSON)
      .put("/contract-signed")
      .then()
      .statusCode(400);

    // then
    Mockito.verify(tokenService, times(1)).updateContractSigned(anyString(), anyString());
  }

  @Test
  @TestSecurity(user = "userJwt")
  void reportContractSignedTest_OK() {
    // given
    final String onboardingId = "onboardingId";

    Uni<ContractSignedReport> response = Uni.createFrom().item(ContractSignedReport.cades(true));
    when(tokenService.reportContractSigned(onboardingId)).thenReturn(response);

    // when
    given()
      .when()
      .queryParam("onboardingId", onboardingId)
      .contentType(MediaType.APPLICATION_JSON)
      .get("/contract-report")
      .then()
      .statusCode(200);

    // then
    Mockito.verify(tokenService, times(1)).reportContractSigned(anyString());
  }
}
