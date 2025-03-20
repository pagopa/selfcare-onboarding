package it.pagopa.selfcare.onboarding.service;

import static it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.onboarding.controller.response.ContractSignedReport;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import jakarta.inject.Inject;
import java.io.File;
import java.util.*;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class TokenServiceDefaultTest {

  @Inject
  TokenServiceDefault tokenService;
  @InjectMock
  AzureBlobClient azureBlobClient;
  @InjectMock
  SignatureService signatureService;

  private static final String onboardingId = "onboardingId";

  @Test
  void getToken() {
    ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
    when(queryPage.list()).thenReturn(Uni.createFrom().item(List.of(new Token())));

    PanacheMock.mock(Token.class);
    when(Token.find("onboardingId", onboardingId))
      .thenReturn(queryPage);

    tokenService.getToken(onboardingId)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();
  }

  @Test
  void retrieveContractNotSigned() {
    Token token = new Token();
    token.setContractFilename("fileName");
    token.setType(TokenType.INSTITUTION);
    ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);

    PanacheMock.mock(Token.class);
    when(Token.findById(onboardingId))
      .thenReturn(Uni.createFrom().item(token));

    when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(new File("fileName"));

    UniAssertSubscriber<RestResponse<File>> subscriber = tokenService.retrieveContract(onboardingId, false)
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    RestResponse<File> actual = subscriber.awaitItem().getItem();
    assertNotNull(actual);
    assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
  }

  @Test
  void retrieveContractSignedTest() {
    // given
    Token token = new Token();
    token.setContractSigned("parties/docs/test-path/NomeDocumentoProva.pdf");
    token.setType(TokenType.INSTITUTION);
    ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);

    PanacheMock.mock(Token.class);
    when(Token.findById(onboardingId))
        .thenReturn(Uni.createFrom().item(token));

    when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(new File("fileName"));

    // when
    UniAssertSubscriber<RestResponse<File>> subscriber = tokenService.retrieveContract(onboardingId, true)
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    // then
    RestResponse<File> actual = subscriber.awaitItem().getItem();
    assertNotNull(actual);
    assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
  }

  @Test
  void retrieveAttachment() {
    final String filename = "filename";
    Token token = new Token();
    token.setContractFilename(filename);
    token.setName(filename);
    ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
    when(queryPage.firstResult()).thenReturn(Uni.createFrom().item(token));

    PanacheMock.mock(Token.class);
    when(Token.find("onboardingId = ?1 and type = ?2 and name = ?3", onboardingId, ATTACHMENT.name(), filename))
      .thenReturn(queryPage);

    when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(new File("fileName"));

    UniAssertSubscriber<RestResponse<File>> subscriber = tokenService.retrieveAttachment(onboardingId, filename)
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    RestResponse<File> actual = subscriber.awaitItem().getItem();
    assertNotNull(actual);
    assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
  }

  @Test
  void getTokenAttachments() {
    final String filename = "filename";
    Token token = new Token();
    token.setContractFilename(filename);
    token.setName(filename);
    ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
    when(queryPage.stream()).thenReturn(Multi.createFrom().item(token));

    PanacheMock.mock(Token.class);
    when(Token.find("onboardingId = ?1 and type = ?2", onboardingId, ATTACHMENT.name()))
      .thenReturn(queryPage);

    UniAssertSubscriber<List<String>> subscriber = tokenService.getAttachments(onboardingId)
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    List<String> actual = subscriber.awaitItem().getItem();
    assertNotNull(actual);
    Assertions.assertFalse(actual.isEmpty());
    assertEquals(filename, actual.get(0));
  }

  @Test
  void updateContractSignedTest_OK() {
    // given
    String onboardingId = "testContractSigned";
    String documentSignedPath = "/test/";
    Map<String, Object> testMap = new HashMap<>();
    testMap.put("contractSigned", documentSignedPath);

    ReactivePanacheUpdate queryPage = mock(ReactivePanacheUpdate.class);
    PanacheMock.mock(Token.class);
    when(queryPage.where(anyString(), anyString())).thenReturn(Uni.createFrom().item(1L));
    when(Token.update(QueryUtils.buildUpdateDocument(testMap))).thenReturn(queryPage);

    // when
    UniAssertSubscriber<Long> result = tokenService.updateContractSigned(onboardingId, documentSignedPath).subscribe()
      .withSubscriber(UniAssertSubscriber.create());

    // then
    assertNotNull(result);
    assertEquals(1L, result.assertCompleted().awaitItem().getItem());

  }

  @Test
  void updateContractSignedTest_KO() {
    // given
    String onboardingId = "testContractSigned";
    String documentSignedPath = "/test/";

    Map<String, Object> testMap = new HashMap<>();
    testMap.put("contractSigned", documentSignedPath);

    ReactivePanacheUpdate queryPage = mock(ReactivePanacheUpdate.class);
    PanacheMock.mock(Token.class);
    when(queryPage.where(anyString(), anyString())).thenReturn(Uni.createFrom().item(Long.valueOf("0")));
    when(Token.update(QueryUtils.buildUpdateDocument(testMap))).thenReturn(queryPage);

    // when
    UniAssertSubscriber<Long> result = tokenService.updateContractSigned(onboardingId, documentSignedPath).subscribe()
      .withSubscriber(UniAssertSubscriber.create());

    // then
    assertNotNull(result);
    assertEquals("Token with id testContractSigned not found or already deleted", result.getFailure().getMessage());

  }


  @Test
  void reportContractSignedTest_OK() {
    Token token = new Token();
    token.setContractFilename("fileName");
    token.setType(TokenType.INSTITUTION);
    ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);

    PanacheMock.mock(Token.class);
    when(Token.findById(onboardingId))
      .thenReturn(Uni.createFrom().item(token));

    when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(new File("fileName"));

    UniAssertSubscriber<ContractSignedReport> subscriber = tokenService.reportContractSigned(onboardingId)
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    ContractSignedReport actual = subscriber.awaitItem().getItem();
    assertNotNull(actual);
    //assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
  }

  private Token createDummyToken() {
    Token token = new Token();
    token.setId(UUID.randomUUID().toString());
    token.setProductId("prod-id");
    token.setContractSigned("file");
    return token;
  }

  private void mockFindToken(UniAsserter asserter, String tokenId) {
    Token token = new Token();
    token.setChecksum("actual-checksum");
    asserter.execute(() -> PanacheMock.mock(Token.class));
    asserter.execute(() -> when(Token.list("_id", tokenId))
      .thenReturn(Uni.createFrom().item(List.of(token))));
  }

  @Test
  @RunOnVertxContext
  void completeWithoutSignatureValidation(UniAsserter asserter) {
    Token token = createDummyToken();
    asserter.execute(() -> PanacheMock.mock(Token.class));
    asserter.execute(() -> when(Token.findByIdOptional(any()))
      .thenReturn(Uni.createFrom().item(Optional.of(token))));

    mockFindToken(asserter, token.getId());

    //Mock contract signature fail
    asserter.execute(() -> doNothing()
      .when(signatureService)
      .verifySignature(any()));

    asserter.assertThat(() -> tokenService.reportContractSigned(token.getId()),
      Assertions::assertNotNull);
  }
}
