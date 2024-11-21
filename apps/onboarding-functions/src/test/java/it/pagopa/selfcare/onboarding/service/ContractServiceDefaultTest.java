package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.config.AzureStorageConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.config.PagoPaSignatureConfig;
import it.pagopa.selfcare.onboarding.crypto.PadesSignService;
import it.pagopa.selfcare.onboarding.entity.*;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class ContractServiceDefaultTest {

  @Inject AzureStorageConfig azureStorageConfig;

  @InjectMock AzureBlobClient azureBlobClient;
  PadesSignService padesSignService;

  @Inject ContractService contractService;

  @Inject PagoPaSignatureConfig pagoPaSignatureConfig;

  @Inject MailTemplatePlaceholdersConfig mailTemplatePlaceholdersConfig;

  static final String PRODUCT_NAME_EXAMPLE = "product-name";
  static final String LOGO_PATH = "logo-path";
  static final String PDF_FORMAT_FILENAME = "%s_accordo_adesione.pdf";

  @BeforeEach
  void setup() {
    padesSignService = mock(PadesSignService.class);
    contractService =
        new ContractServiceDefault(
            azureStorageConfig,
            azureBlobClient,
            padesSignService,
            pagoPaSignatureConfig,
            mailTemplatePlaceholdersConfig,
            LOGO_PATH,
            true);
  }

  private Onboarding createOnboarding() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("example");
    onboarding.setProductId("productId");
    onboarding.setUsers(List.of());

    Institution institution = new Institution();
    institution.setInstitutionType(InstitutionType.PSP);
    institution.setDescription("42");

    institution.setRea("rea");
    institution.setBusinessRegisterPlace("place");
    institution.setShareCapital("10000");
    onboarding.setInstitution(institution);

    User user = new User();
    user.setId(UUID.randomUUID().toString());
    user.setUserMailUuid("setUserMailUuid");
    onboarding.setUsers(List.of(user));
    return onboarding;
  }

  AggregateInstitution createAggregateInstitutionIO(int number) {
    AggregateInstitution aggregateInstitution = new AggregateInstitution();
    aggregateInstitution.setTaxCode(String.format("taxCode%s", number));
    aggregateInstitution.setOriginId(String.format("originId%s", number));
    aggregateInstitution.setDescription(String.format("description%s", number));
    aggregateInstitution.setVatNumber(String.format("vatNumber%s", number));
    aggregateInstitution.setAddress(String.format("address%s", number));
    aggregateInstitution.setCity(String.format("city%s", number));
    aggregateInstitution.setCounty(String.format("county%s", number));
    aggregateInstitution.setDigitalAddress(String.format("pec%s", number));
    return aggregateInstitution;
  }

  AggregateInstitution createAggregateInstitutionAOO_IO(int number) {
    AggregateInstitution aggregateInstitution = createAggregateInstitutionIO(number);
    aggregateInstitution.setSubunitType("AOO");
    aggregateInstitution.setSubunitCode(String.format("code%s", number));
    return aggregateInstitution;
  }

  AggregateInstitution createAggregateInstitutionPagoPa(int number) {
    AggregateInstitution aggregateInstitution = new AggregateInstitution();
    aggregateInstitution.setTaxCode(String.format("taxCode%s", number));
    aggregateInstitution.setDescription(String.format("description%s", number));
    aggregateInstitution.setVatNumber(String.format("vatNumber%s", number));
    aggregateInstitution.setAddress(String.format("address%s", number));
    aggregateInstitution.setCity(String.format("city%s", number));
    aggregateInstitution.setCounty(String.format("county%s", number));
    aggregateInstitution.setDigitalAddress(String.format("pec%s", number));
    aggregateInstitution.setTaxCodePT(String.format("taxCodePT%s", number));
    aggregateInstitution.setDescriptionPT(String.format("descriptionPT%s", number));
    aggregateInstitution.setIban(String.format("iban%s", number));
    aggregateInstitution.setService(String.format("service%s", number));
    aggregateInstitution.setSyncAsyncMode(String.format("mode%s", number));
    return aggregateInstitution;
  }

  OnboardingWorkflow createOnboardingWorkflowIO() {
    Onboarding onboarding = createOnboarding();
    onboarding.setProductId("prod-io");
    List<AggregateInstitution> aggregateInstitutionList = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      AggregateInstitution aggregateInstitution = createAggregateInstitutionIO(i);
      aggregateInstitutionList.add(aggregateInstitution);
    }

    for (int i = 6; i <= 10; i++) {
      AggregateInstitution aggregateInstitution = createAggregateInstitutionAOO_IO(i);
      aggregateInstitutionList.add(aggregateInstitution);
    }

    onboarding.setAggregates(aggregateInstitutionList);

    return new OnboardingWorkflowAggregator(onboarding, "string");
  }

  OnboardingWorkflow createOnboardingWorkflowPagoPa() {
    Onboarding onboarding = createOnboarding();
    onboarding.setProductId("prod-pagopa");
    List<AggregateInstitution> aggregateInstitutionList = new ArrayList<>();

    for (int i = 1; i <= 7; i++) {
      AggregateInstitution aggregateInstitution = createAggregateInstitutionPagoPa(i);
      aggregateInstitutionList.add(aggregateInstitution);
    }

    onboarding.setAggregates(aggregateInstitutionList);

    return new OnboardingWorkflowAggregator(onboarding, "string");
  }

  UserResource createDummyUserResource(String id, String userMailUuid) {
    UserResource validManager = new UserResource();
    validManager.setId(UUID.fromString(id));
    CertifiableFieldResourceOfstring emailCert = new CertifiableFieldResourceOfstring();
    emailCert.setValue("email");
    WorkContactResource workContact = new WorkContactResource();
    workContact.setEmail(emailCert);
    Map<String, WorkContactResource> map = new HashMap<>();
    map.put(userMailUuid, workContact);

    validManager.setWorkContacts(map);
    return validManager;
  }

  @Test
  void createContractPDF() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";
    final String productNameAccent = "Interoperabilità";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
        createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    File contract =
        contractService.createContractPDF(
            contractFilepath,
            onboarding,
            manager,
            List.of(),
            productNameAccent,
            PDF_FORMAT_FILENAME);

    assertNotNull(contract);

    ArgumentCaptor<String> captorFilename = ArgumentCaptor.forClass(String.class);
    verify(azureBlobClient, times(1)).uploadFile(any(), captorFilename.capture(), any());
    assertEquals("Interoperabilita_accordo_adesione.pdf", captorFilename.getValue());
  }

  @Test
  void createAttachmentPDF() {
    final String contractFilepath = "attachment";
    final String contractHtml = "attachment";
    final String productNameAccent = "Interoperabilità";
    final String pdfFormatFile = "checklist";

    Onboarding onboarding = createOnboarding();

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    File attachmentPDF =
        contractService.createAttachmentPDF(
            contractFilepath, onboarding, productNameAccent, pdfFormatFile);

    assertNotNull(attachmentPDF);

    ArgumentCaptor<String> captorFilename = ArgumentCaptor.forClass(String.class);
    verify(azureBlobClient, times(1)).uploadFile(any(), captorFilename.capture(), any());
    assertEquals("Interoperabilita_checklist.pdf", captorFilename.getValue());
  }

  @Test
  void createContractPDFSA() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
        createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.getInstitution().setInstitutionType(InstitutionType.SA);

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    assertNotNull(
        contractService.createContractPDF(
            contractFilepath,
            onboarding,
            manager,
            List.of(),
            PRODUCT_NAME_EXAMPLE,
            PDF_FORMAT_FILENAME));
  }

  @Test
  void createContractPDFForECAndProdPagoPA() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
        createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.getInstitution().setInstitutionType(InstitutionType.PA);
    onboarding.setProductId("prod-pagopa");

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    assertNotNull(
        contractService.createContractPDF(
            contractFilepath,
            onboarding,
            manager,
            List.of(),
            PRODUCT_NAME_EXAMPLE,
            PDF_FORMAT_FILENAME));
  }

  @ParameterizedTest
  @ValueSource(strings = {"prod-io", "prod-io-sign"})
  void createContractPDFForProdIo(String productId) {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
        createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.getInstitution().setInstitutionType(InstitutionType.PA);
    onboarding.setProductId(productId);

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    assertNotNull(
        contractService.createContractPDF(
            contractFilepath,
            onboarding,
            manager,
            List.of(),
            PRODUCT_NAME_EXAMPLE,
            PDF_FORMAT_FILENAME));
  }

  @Test
  void createContractPDFAndSigned() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
        createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());

    pagoPaSignatureConfig = Mockito.spy(this.pagoPaSignatureConfig);
    when(pagoPaSignatureConfig.source()).thenReturn("local");
    contractService =
        new ContractServiceDefault(
            azureStorageConfig,
            azureBlobClient,
            padesSignService,
            pagoPaSignatureConfig,
            mailTemplatePlaceholdersConfig,
            "logo-path",
            true);

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

    Mockito.doNothing().when(padesSignService).padesSign(any(), any(), any());

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    assertNotNull(
        contractService.createContractPDF(
            contractFilepath,
            onboarding,
            manager,
            List.of(),
            PRODUCT_NAME_EXAMPLE,
            PDF_FORMAT_FILENAME));
  }

  @Test
  void loadContractPDF() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();

    File pdf =
        new File(
            Objects.requireNonNull(
                    getClass().getClassLoader().getResource("application.properties"))
                .getFile());

    Mockito.when(azureBlobClient.getFileAsPdf(contractFilepath)).thenReturn(pdf);

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    assertNotNull(
        contractService.loadContractPDF(
            contractFilepath, onboarding.getId(), PRODUCT_NAME_EXAMPLE));
  }

  @Test
  void retrieveContractNotSigned() {

    Onboarding onboarding = createOnboarding();
    OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowInstitution();
    onboardingWorkflow.setOnboarding(onboarding);

    File pdf = mock(File.class);
    Mockito.when(azureBlobClient.getFileAsPdf(any())).thenReturn(pdf);

    contractService.retrieveContractNotSigned(onboardingWorkflow, PRODUCT_NAME_EXAMPLE);

    ArgumentCaptor<String> filepathActual = ArgumentCaptor.forClass(String.class);
    Mockito.verify(azureBlobClient, times(1)).getFileAsPdf(filepathActual.capture());
    assertTrue(filepathActual.getValue().contains(onboarding.getId()));
    assertTrue(filepathActual.getValue().contains(PRODUCT_NAME_EXAMPLE));
  }

  @Test
  void getLogoFile() {
    Mockito.when(azureBlobClient.getFileAsText(any())).thenReturn("example");

    contractService.getLogoFile();

    Mockito.verify(azureBlobClient, times(1)).getFileAsText(any());
  }

  @Test
  void uploadCsvAggregatesIO() {
    final String contractHtml = "contract";

    OnboardingWorkflow onboardingWorkflow = createOnboardingWorkflowIO();

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    contractService.uploadAggregatesCsv(onboardingWorkflow);

    Mockito.verify(azureBlobClient, times(1)).uploadFile(any(), any(), any());
  }

  @Test
  void uploadCsvAggregatesPagoPa() {
    final String contractHtml = "contract";

    OnboardingWorkflow onboardingWorkflow = createOnboardingWorkflowPagoPa();

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    contractService.uploadAggregatesCsv(onboardingWorkflow);

    Mockito.verify(azureBlobClient, times(1)).uploadFile(any(), any(), any());
  }

  @Test
  void uploadCsvAggregatesProdPn() {
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    onboarding.setProductId("prod-pn");
    OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowAggregator(onboarding, "string");

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    contractService.uploadAggregatesCsv(onboardingWorkflow);

    Mockito.verify(azureBlobClient, times(1)).uploadFile(any(), any(), any());
  }

  @Test
  void uploadCsvAggregatesProductNotValid() {
    Onboarding onboarding = createOnboarding();
    onboarding.setProductId("prod-interop");
    OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowAggregator(onboarding, "string");

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> contractService.uploadAggregatesCsv(onboardingWorkflow));
  }

  @Test
  void createContractPRV() {
    // given
    String contractFilepath = "contract";
    String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
        createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.getInstitution().setInstitutionType(InstitutionType.PRV);
    onboarding.setProductId("prod-pagopa");

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);
    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    // when
    File result =
        contractService.createContractPDF(
            contractFilepath,
            onboarding,
            manager,
            List.of(),
            PRODUCT_NAME_EXAMPLE,
            PDF_FORMAT_FILENAME);

    // then
    assertNotNull(result);
    Mockito.verify(azureBlobClient, Mockito.times(1)).getFileAsText(contractFilepath);
    Mockito.verify(azureBlobClient, Mockito.times(1)).uploadFile(any(), any(), any());
    Mockito.verifyNoMoreInteractions(azureBlobClient);
  }
}
