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
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.onboarding.controller.response.ContractSignedReport;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.onboarding.service.impl.TokenServiceDefault;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
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

  @InjectMock
  ProductService productService;

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
  void retrieveContractSignedKOTest() {
    // given
    Token token = new Token();
    token.setContractSigned("parties/docs/test-path/NomeDocumentoProva.p7m");
    token.setType(TokenType.INSTITUTION);
    ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);

    PanacheMock.mock(Token.class);
    when(Token.findById(onboardingId))
      .thenReturn(Uni.createFrom().item(token));

    ClassLoader classLoader = getClass().getClassLoader();
    String resourcePath = Objects.requireNonNull(classLoader.getResource("documents/contract_error.p7m")).getPath();

    when(azureBlobClient.retrieveFile(anyString())).thenReturn(new File(resourcePath));

    // when
    UniAssertSubscriber<RestResponse<File>> subscriber = tokenService.retrieveSignedFile(onboardingId)
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    // then
    RestResponse<File> actual = subscriber.awaitItem().getItem();
    assertNotNull(actual);
    assertEquals(RestResponse.Status.NOT_FOUND.getStatusCode(), actual.getStatus());
  }

  @Test
  void retrieveContractSignedPdfOKTest() {
    // given
    Token token = new Token();
    token.setContractSigned("parties/docs/test-path/NomeDocumentoProva.pdf");
    token.setType(TokenType.INSTITUTION);
    ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);

    PanacheMock.mock(Token.class);
    when(Token.findById(onboardingId))
      .thenReturn(Uni.createFrom().item(token));

    ClassLoader classLoader = getClass().getClassLoader();
    String resourcePath = Objects.requireNonNull(classLoader.getResource("documents/test.pdf")).getPath();

    when(azureBlobClient.retrieveFile(anyString())).thenReturn(new File(resourcePath));

    // when
    UniAssertSubscriber<RestResponse<File>> subscriber = tokenService.retrieveSignedFile(onboardingId)
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    // then
    RestResponse<File> actual = subscriber.awaitItem().getItem();
    assertNotNull(actual);
    assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
  }

  @Test
  void retrieveContractSignedP7mOKTest() {
    // given
    Token token = new Token();
    token.setContractSigned("parties/docs/test-path/NomeDocumentoProva.p7m");
    token.setType(TokenType.INSTITUTION);

    PanacheMock.mock(Token.class);
    when(Token.findById(onboardingId))
      .thenReturn(Uni.createFrom().item(token));

    ClassLoader classLoader = getClass().getClassLoader();
    String resourcePath = Objects.requireNonNull(classLoader.getResource("documents/test.pdf.p7m")).getPath();
    when(azureBlobClient.retrieveFile(anyString())).thenReturn(new File(resourcePath));

    String resourceExtractedPath = Objects.requireNonNull(classLoader.getResource("documents/test.pdf")).getPath();
    when(signatureService.extractFile(any())).thenReturn(new File(resourceExtractedPath));

    // when
    UniAssertSubscriber<RestResponse<File>> subscriber = tokenService.retrieveSignedFile(onboardingId)
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    // then
    RestResponse<File> actual = subscriber.awaitItem().getItem();
    assertNotNull(actual);
    assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
  }

  @Test
  void retrieveAttachment_onboardingNotFound() {
    PanacheMock.mock(Onboarding.class);
    when(Onboarding.findById(anyString()))
            .thenReturn(Uni.createFrom().nullItem());

    UniAssertSubscriber<RestResponse<File>> subscriber =
            tokenService.retrieveAttachment("id", "file")
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitFailure()
            .assertFailedWith(ResourceNotFoundException.class);
  }


  @Test
  void retrieveAttachmentGeneratedSuccess() {
    final String onboardingId = "onboardingId";
    final String filename = "filename.pdf";
    final String productId = "productId";
    final String institutionType = "PA";

    Onboarding onboarding = new Onboarding();
    onboarding.setId(onboardingId);
    onboarding.setProductId(productId);

    Institution institution = new Institution();
    institution.setInstitutionType(InstitutionType.PA);
    onboarding.setInstitution(institution);

    PanacheMock.mock(Onboarding.class);
    when(Onboarding.findById(onboardingId)).thenReturn(Uni.createFrom().item(onboarding));

    AttachmentTemplate attachment = new AttachmentTemplate();
    attachment.setName(filename);
    attachment.setGenerated(true);
    Product product = createProductWithAttachment(institutionType, attachment);

    when(productService.getProductIsValid(productId)).thenReturn(product);

    Token token = new Token();
    token.setContractFilename(filename);
    token.setName(filename);

    ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
    when(queryPage.firstResult()).thenReturn(Uni.createFrom().item(token));

    PanacheMock.mock(Token.class);
    when(Token.find("onboardingId = ?1 and type = ?2 and name = ?3", onboardingId, ATTACHMENT.name(), filename))
            .thenReturn(queryPage);

    File file = new File("test.pdf");
    when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(file);

    UniAssertSubscriber<RestResponse<File>> subscriber =
            tokenService.retrieveAttachment(onboardingId, filename)
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

    RestResponse<File> response = subscriber.awaitItem().getItem();

    assertNotNull(response);
    assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
    assertEquals(file, response.getEntity());

    verify(productService).getProductIsValid(productId);
    verify(azureBlobClient).getFileAsPdf(anyString());
  }

  @Test
  void retrieveAttachmentGeneratedFalseSuccess() {
    final String onboardingId = "onboardingId";
    final String filename = "filename.pdf";
    final String productId = "productId";
    final String institutionType = "PA";

    Onboarding onboarding = new Onboarding();
    onboarding.setId(onboardingId);
    onboarding.setProductId(productId);

    Institution institution = new Institution();
    institution.setInstitutionType(InstitutionType.PA);
    onboarding.setInstitution(institution);

    PanacheMock.mock(Onboarding.class);
    when(Onboarding.findById(onboardingId)).thenReturn(Uni.createFrom().item(onboarding));

    AttachmentTemplate attachment = new AttachmentTemplate();
    attachment.setName(filename);
    attachment.setGenerated(false);
    attachment.setTemplatePath("templatePath");
    Product product = createProductWithAttachment(institutionType, attachment);

    when(productService.getProductIsValid(productId)).thenReturn(product);

    File file = new File("test.pdf");
    when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(file);

    UniAssertSubscriber<RestResponse<File>> subscriber =
            tokenService.retrieveAttachment(onboardingId, filename)
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

    RestResponse<File> response = subscriber.awaitItem().getItem();

    assertNotNull(response);
    assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
    assertEquals(file, response.getEntity());

    verify(productService).getProductIsValid(productId);
    verify(azureBlobClient).getFileAsPdf(anyString());
  }

  @Test
  void uploadAttachmentErrorDigest() {

    final String onboardingId = "onboardingId";
    final String filename = "filename.pdf";
    final String productId = "productId";

    Onboarding onboarding = new Onboarding();
    onboarding.setId(onboardingId);
    onboarding.setProductId(productId);

    Institution institution = new Institution();
    institution.setInstitutionType(InstitutionType.PA);
    onboarding.setInstitution(institution);

    PanacheMock.mock(Onboarding.class);
    when(Onboarding.findById(onboardingId))
            .thenReturn(Uni.createFrom().item(onboarding));

    AttachmentTemplate attachment = new AttachmentTemplate();
    attachment.setName(filename);
    attachment.setTemplatePath("template.pdf");
    attachment.setGenerated(true);

    Product product = createProductWithAttachment("PA", attachment);
    when(productService.getProductIsValid(productId)).thenReturn(product);

    File uploadedFile = new File("src/test/resources/test.pdf");

    File originalFile = new File("src/test/resources/original.pdf");

    when(azureBlobClient.getFileAsPdf(anyString()))
            .thenReturn(originalFile);

    FormItem formItem = FormItem.builder()
            .file(uploadedFile)
            .fileName(filename)
            .build();

    UniAssertSubscriber<Void> subscriber =
            tokenService.uploadAttachment(onboardingId, formItem, filename)
                    .subscribe()
                    .withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitFailure();

    subscriber.assertFailedWith(InvalidRequestException.class);

    verify(productService).getProductIsValid(productId);
    verify(azureBlobClient).getFileAsPdf(anyString());
  }

  @Test
  @RunOnVertxContext
  void uploadAttachmentSuccess(UniAsserter asserter) {

    final String onboardingId = "onboardingId";
    final String filename = "filename.pdf";
    final String productId = "productId";

    Onboarding onboarding = new Onboarding();
    onboarding.setId(onboardingId);
    onboarding.setProductId(productId);

    Institution institution = new Institution();
    institution.setInstitutionType(InstitutionType.PA);
    onboarding.setInstitution(institution);

    asserter.execute(() -> PanacheMock.mock(Onboarding.class));
    asserter.execute(() -> when(Onboarding.findById(anyString()))
            .thenReturn(Uni.createFrom().item(onboarding)));

    AttachmentTemplate attachment = new AttachmentTemplate();
    attachment.setName(filename);
    attachment.setTemplatePath("template.pdf");
    attachment.setGenerated(true);

    Product product = createProductWithAttachment("PA", attachment);
    asserter.execute(() -> when(productService.getProductIsValid(anyString()))
            .thenReturn(product));

    File pdf = new File("src/test/resources/test.pdf");

    asserter.execute(() -> when(azureBlobClient.getFileAsPdf(anyString()))
            .thenReturn(pdf));

    asserter.execute(() -> when(azureBlobClient
            .uploadFile(anyString(), anyString(), any(byte[].class)))
            .thenReturn("mocked-filepath"));

    FormItem formItem = FormItem.builder()
            .file(pdf)
            .fileName(filename)
            .build();

    mockPersistToken(asserter);

    asserter.assertThat(
            () -> tokenService.uploadAttachment(onboardingId, formItem, filename),
            result -> {
              verify(productService).getProductIsValid(anyString());
              verify(azureBlobClient).getFileAsPdf(anyString());
              verify(azureBlobClient).uploadFile(anyString(), anyString(), any(byte[].class));
            }
    );
  }

  void mockPersistToken(UniAsserter asserter) {
    asserter.execute(() -> PanacheMock.mock(Token.class));
    asserter.execute(() -> when(Token.persist(any(Token.class), any()))
            .thenAnswer(arg -> {
              Token token = (Token) arg.getArguments()[0];
              token.setId(UUID.randomUUID().toString());
              return Uni.createFrom().nullItem();
            }));
  }

  private Product createProductWithAttachment(String institutionType, AttachmentTemplate attachment) {
    Product product = new Product();

    ContractTemplate contractTemplate = new ContractTemplate();
    contractTemplate.setAttachments(List.of(attachment));

    Map<String, ContractTemplate> institutionContractMappings = new HashMap<>();
    institutionContractMappings.put(institutionType, contractTemplate);

    product.setInstitutionContractMappings(institutionContractMappings);
    return product;
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
  void reportContractSignedTest(UniAsserter asserter) {
    Token token = createDummyToken();
    asserter.execute(() -> PanacheMock.mock(Token.class));
    asserter.execute(() -> when(Token.findByIdOptional(any()))
      .thenReturn(Uni.createFrom().item(Optional.of(token))));

    mockFindToken(asserter, token.getId());

    when(signatureService.verifySignature(any())).thenReturn(true);

    asserter.assertThat(() -> tokenService.reportContractSigned(token.getId()),
      Assertions::assertNotNull);
  }
}
