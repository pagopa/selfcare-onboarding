package it.pagopa.selfcare.onboarding.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.config.AzureStorageConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.config.PagoPaSignatureConfig;
import it.pagopa.selfcare.onboarding.crypto.PadesSignService;
import it.pagopa.selfcare.onboarding.crypto.entity.SignatureInformation;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.utils.ClassPathStream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.text.StringSubstitutor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

import static it.pagopa.selfcare.onboarding.common.ProductId.*;
import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_WORKS_FIELD_LIST;
import static it.pagopa.selfcare.onboarding.utils.GenericError.*;
import static it.pagopa.selfcare.onboarding.utils.PdfMapper.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.CONTRACT_FILENAME_FUNC;

@ApplicationScoped
public class ContractServiceDefault implements ContractService {

    @Inject
    @RestClient
    UserApi userRegistryApi;

    private static final Logger log = LoggerFactory.getLogger(ContractServiceDefault.class);
    public static final String PAGOPA_SIGNATURE_DISABLED = "disabled";
    private final AzureStorageConfig azureStorageConfig;
    private final AzureBlobClient azureBlobClient;
    private final PadesSignService padesSignService;
    private final PagoPaSignatureConfig pagoPaSignatureConfig;
    private final MailTemplatePlaceholdersConfig templatePlaceholdersConfig;

    Boolean isLogoEnable;

    private final String logoPath;

    private static final String INSTITUTION_DESCRIPTION_HEADER = "Ragione Sociale";
    private static final String PEC_HEADER = "PEC";
    private static final String FISCAL_CODE_HEADER = "Codice Fiscale";
    private static final String PIVA_HEADER = "P.IVA";
    private static final String REGISTERED_OFFICE_ADDRESS = "Sede legale - Indirizzo";
    private static final String REGISTERED_OFFICE_CITY = "Sede legale - Citta'";
    private static final String REGISTERED_OFFICE_COUNTY = "Sede legale - Provincia (Sigla)";



    private static final String[] CSV_HEADERS_IO = {
            INSTITUTION_DESCRIPTION_HEADER,
            PEC_HEADER,
            FISCAL_CODE_HEADER,
            PIVA_HEADER,
            REGISTERED_OFFICE_ADDRESS,
            REGISTERED_OFFICE_CITY,
            REGISTERED_OFFICE_COUNTY,
            "Codice IPA",
            "AOO/UO",
            "Codice Univoco"
    };

    private static final String[] CSV_HEADERS_PAGOPA = {
            INSTITUTION_DESCRIPTION_HEADER,
            PEC_HEADER,
            FISCAL_CODE_HEADER,
            PIVA_HEADER,
            REGISTERED_OFFICE_ADDRESS,
            REGISTERED_OFFICE_CITY,
            REGISTERED_OFFICE_COUNTY,
            "Ragione Sociale Partener Tecnologico",
            "Codice Fiscale Partner Tecnologico",
            "IBAN",
            "Servizio",
            "Modalità Sincrona/Asincrona"
    };

    private static final String[] CSV_HEADERS_SEND = {
            INSTITUTION_DESCRIPTION_HEADER,
            PEC_HEADER,
            FISCAL_CODE_HEADER,
            PIVA_HEADER,
            "Codice SDI",
            REGISTERED_OFFICE_ADDRESS,
            REGISTERED_OFFICE_CITY,
            REGISTERED_OFFICE_COUNTY,
            "Codice IPA",
            "AOO/UO",
            "Codice Univoco",
            "Nome Amministratore Ente Aggregato",
            "Cognome Amministratore Ente Aggregato",
            "Codice Fiscale Amministratore Ente Aggregato",
            "email Amministratore Ente Aggregato"
    };

    private static final String LEGAL_SENTENCE_IO =
            "*** Il presente file non puo' essere modificato se non unitamente al "
                    + "documento \"Allegato 3\" in cui e' incoporato. Ogni modifica, alterazione e variazione dei dati e delle "
                    + "informazioni del presente file non accompagnata dall'invio e dalla firma digitale dell'intero documento "
                    + "\"Allegato 3\" e' da considerarsi priva di ogni efficacia ai sensi di legge e ai fini del presente Accordo. "
                    + "In caso di discrepanza tra i dati contenuti nel presente file e i dati contenuti nell'Allegato 3, "
                    + "sara' data prevalenza a questi ultimi.";

    private static final Function<AggregateInstitution, List<Object>> IO_MAPPER =
            institution ->
                    Arrays.asList(
                            institution.getDescription(),
                            institution.getDigitalAddress(),
                            institution.getTaxCode(),
                            institution.getVatNumber(),
                            institution.getAddress(),
                            institution.getCity(),
                            institution.getCounty(),
                            Optional.ofNullable(institution.getSubunitType())
                                    .map(originId -> "")
                                    .orElse(institution.getOriginId()),
                            institution.getSubunitType(),
                            institution.getSubunitCode());

    private static final Function<AggregateInstitution, List<Object>> PAGOPA_MAPPER =
            institution ->
                    Arrays.asList(
                            institution.getDescription(),
                            institution.getDigitalAddress(),
                            institution.getTaxCode(),
                            institution.getVatNumber(),
                            institution.getAddress(),
                            institution.getCity(),
                            institution.getCounty(),
                            institution.getDescriptionPT(),
                            institution.getTaxCodePT(),
                            institution.getIban(),
                            institution.getService(),
                            institution.getSyncAsyncMode());

    public static Function<AggregateInstitution, List<Object>> sendMapper(UserResource userInfo, User user) {
        return institution -> Arrays.asList(
                institution.getDescription(),
                institution.getDigitalAddress(),
                institution.getTaxCode(),
                institution.getVatNumber(),
                institution.getRecipientCode(),
                institution.getAddress(),
                institution.getCity(),
                institution.getCounty(),
                Optional.ofNullable(institution.getSubunitType())
                        .map(originId -> "")
                        .orElse(institution.getOriginId()),
                institution.getSubunitType(),
                institution.getSubunitCode(),
                userInfo.getName().getValue(),
                userInfo.getFamilyName().getValue(),
                userInfo.getFiscalCode(),
                userInfo.getWorkContacts().get(user.getUserMailUuid()).getEmail().getValue()
        );
    }

    public ContractServiceDefault(
            AzureStorageConfig azureStorageConfig,
            AzureBlobClient azureBlobClient,
            PadesSignService padesSignService,
            PagoPaSignatureConfig pagoPaSignatureConfig,
            MailTemplatePlaceholdersConfig templatePlaceholdersConfig,
            @ConfigProperty(name = "onboarding-functions.logo-path") String logoPath,
            @ConfigProperty(name = "onboarding-functions.logo-enable") Boolean isLogoEnable,
            @RestClient UserApi userRegistryApi) {
        this.azureStorageConfig = azureStorageConfig;
        this.azureBlobClient = azureBlobClient;
        this.padesSignService = padesSignService;
        this.pagoPaSignatureConfig = pagoPaSignatureConfig;
        this.templatePlaceholdersConfig = templatePlaceholdersConfig;
        this.logoPath = logoPath;
        this.isLogoEnable = isLogoEnable;
        this.userRegistryApi = userRegistryApi;
    }

    /**
     * Creates a PDF contract document from a given contract template file and institution data. Based
     * on @contractTemplatePath it loads contract template as test and replace placeholder using a map
     * <key,value> with institution information. Contract will be stored at
     * parties/docs/{onboardingId}/{productName}_accordo_di_adesione.pdf
     *
     * @param contractTemplatePath The file path to the contract template.
     * @param onboarding           Information related to the onboarding process.
     * @param manager              A user resource representing a valid manager.
     * @param users                A list of user resources.
     * @param productName          Product's name of onboarding.
     * @return A File object representing the created PDF contract document.
     * @throws GenericOnboardingException If an error occurs during PDF generation.
     */
    @Override
    public File createContractPDF(
            String contractTemplatePath,
            Onboarding onboarding,
            UserResource manager,
            List<UserResource> users,
            String productName,
            String pdfFormatFilename) {

        log.info("START - createContractPdf for template: {}", contractTemplatePath);
        // Generate a unique filename for the PDF.
        final String productId = onboarding.getProductId();
        final Institution institution = onboarding.getInstitution();

        try {
            final String[] split = contractTemplatePath.split("\\.");
            final String fileType = split[split.length - 1];

            // If contract template is a PDF, I get without parsing
            File temporaryPdfFile =
                    "pdf".equals(fileType)
                            ? azureBlobClient.getFileAsPdf(contractTemplatePath)
                            : createPdfFileContract(contractTemplatePath, onboarding, manager, users);

            // Define the filename and path for storage.
            final String filename = CONTRACT_FILENAME_FUNC.apply(pdfFormatFilename, productName);
            final String path =
                    String.format("%s%s", azureStorageConfig.contractPath(), onboarding.getId());

            File signedPath = signPdf(temporaryPdfFile, institution.getDescription(), productId);
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(signedPath.toPath()));

            return signedPath;
        } catch (IOException e) {
            throw new GenericOnboardingException(
                    String.format("Can not create contract PDF, message: %s", e.getMessage()));
        }
    }

    public File createAttachmentPDF(
            String attachmentTemplatePath,
            Onboarding onboarding,
            String productName,
            String attachmentName, UserResource userResource) {

        log.info("START - createAttachmentPDF for template: {}", attachmentTemplatePath);

        try {
            final String[] split = attachmentTemplatePath.split("\\.");
            final String fileType = split[split.length - 1];

            // If contract template is a PDF, I get without parsing
            File attachmentPdfFile =
                    "pdf".equals(fileType)
                            ? azureBlobClient.getFileAsPdf(attachmentTemplatePath)
                            : createPdfFileAttachment(attachmentTemplatePath, onboarding, userResource);

            // Define the filename and path for storage.
            final String filename =
                    CONTRACT_FILENAME_FUNC.apply("%s_" + attachmentName + ".pdf", productName);
            final String path =
                    String.format(
                            "%s%s%s", azureStorageConfig.contractPath(), onboarding.getId(), "/attachments");

            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(attachmentPdfFile.toPath()));

            return attachmentPdfFile;
        } catch (IOException e) {
            throw new GenericOnboardingException(
                    String.format("Can not create attachment PDF, message: %s", e.getMessage()));
        }
    }

    private File createPdfFileContract(
            String contractTemplatePath,
            Onboarding onboarding,
            UserResource manager,
            List<UserResource> users)
            throws IOException {
        final String builder =
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        + "_"
                        + UUID.randomUUID()
                        + "_contratto_interoperabilita.";

        final String productId = onboarding.getProductId();
        final Institution institution = onboarding.getInstitution();

        // Read the content of the contract template file.
        String contractTemplateText = azureBlobClient.getFileAsText(contractTemplatePath);
        // Create a temporary PDF file to store the contract.
        Path temporaryPdfFile = Files.createTempFile(builder, ".pdf");
        // Setting baseUrl used to construct aggregates csv url
        String baseUrl = templatePlaceholdersConfig.rejectOnboardingUrlValue();
        // Prepare common data for the contract document.
        Map<String, Object> data = setUpCommonData(manager, users, onboarding, baseUrl);
        // Customize data based on the product and institution type.
        if (PROD_PAGOPA.getValue().equalsIgnoreCase(productId)
                && InstitutionType.PSP == institution.getInstitutionType()) {
            setupPSPData(data, manager, onboarding);
        } else if (PROD_PAGOPA.getValue().equalsIgnoreCase(productId)
                && InstitutionType.PRV == institution.getInstitutionType() || InstitutionType.GPU == institution.getInstitutionType()) {
            setupPRVData(data, onboarding, users);
        } else if (PROD_PAGOPA.getValue().equalsIgnoreCase(productId)
                && InstitutionType.PSP != institution.getInstitutionType()
                && InstitutionType.PT != institution.getInstitutionType()) {
            setECData(data, onboarding);
        } else if (PROD_IO.getValue().equalsIgnoreCase(productId)
                || PROD_IO_PREMIUM.getValue().equalsIgnoreCase(productId)
                || PROD_IO_SIGN.getValue().equalsIgnoreCase(productId)) {
            setupProdIOData(onboarding, data, manager);
        } else if (PROD_PN.getValue().equalsIgnoreCase(productId)) {
            setupProdPNData(data, institution, onboarding.getBilling());
        } else if (PROD_INTEROP.getValue().equalsIgnoreCase(productId)) {
            setupSAProdInteropData(data, institution);
        }
        log.debug("data Map for PDF: {}", data);
        fillPDFAsFile(temporaryPdfFile, contractTemplateText, data);
        return temporaryPdfFile.toFile();
    }

    private File createPdfFileAttachment(String attachmentTemplatePath, Onboarding onboarding, UserResource userResource)
            throws IOException {
        final String builder =
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        + "_"
                        + UUID.randomUUID()
                        + "_allegato_interoperabilita.";

        // Read the content of the contract template file.
        String attachmentTemplateText = azureBlobClient.getFileAsText(attachmentTemplatePath);
        // Create a temporary PDF file to store the contract.
        Path attachmentPdfFile = Files.createTempFile(builder, ".pdf");
        // Prepare common data for the contract document.
        Map<String, Object> data = setUpAttachmentData(onboarding, userResource);

        log.debug("data Map for PDF: {}", data);
        fillPDFAsFile(attachmentPdfFile, attachmentTemplateText, data);
        return attachmentPdfFile.toFile();
    }

    private File signPdf(File pdf, String institutionDescription, String productId)
            throws IOException {
        if (PAGOPA_SIGNATURE_DISABLED.equals(pagoPaSignatureConfig.source())) {
            log.info("Skipping PagoPA contract pdf sign due to global disabling");
            return pdf;
        }

        String signReason =
                pagoPaSignatureConfig
                        .applyOnboardingTemplateReason()
                        .replace("${institutionName}", institutionDescription)
                        .replace("${productName}", productId);

        log.info("Signing input file {} using reason {}", pdf.getName(), signReason);
        Path signedPdf = Files.createTempFile("signed", ".pdf");
        padesSignService.padesSign(pdf, signedPdf.toFile(), buildSignatureInfo(signReason));
        return signedPdf.toFile();
    }

    private SignatureInformation buildSignatureInfo(String signReason) {
        return new SignatureInformation(
                pagoPaSignatureConfig.signer(), pagoPaSignatureConfig.location(), signReason);
    }

    /**
     * Only for test
     */
    @Override
    public File loadContractPDF(
            String contractTemplatePath, String onboardingId, String productName) {
        try {
            File pdf = azureBlobClient.getFileAsPdf(contractTemplatePath);

            final String filename = CONTRACT_FILENAME_FUNC.apply("%s.pdf", productName);
            final String path = String.format("%s/%s", azureStorageConfig.contractPath(), onboardingId);
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(pdf.toPath()));

            return pdf;
        } catch (IOException e) {
            throw new GenericOnboardingException(
                    String.format("Can not load contract PDF, message: %s", e.getMessage()));
        }
    }

    private void fillPDFAsFile(Path file, String contractTemplate, Map<String, Object> data) {
        log.debug("Getting PDF for HTML template...");
        String html = StringSubstitutor.replace(contractTemplate, data);
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useProtocolsStreamImplementation(
                url -> {
                    URI fullUri;
                    try {
                        fullUri = new URI(url);
                        return new ClassPathStream(fullUri.getPath());
                    } catch (URISyntaxException e) {
                        log.error("URISintaxException in ClassPathStreamFactory: ", e);
                        throw new GenericOnboardingException(
                                GENERIC_ERROR.getMessage(), GENERIC_ERROR.getCode());
                    }
                },
                "classpath");
        var doc = Jsoup.parse(html, "UTF-8");
        var dom = W3CDom.convert(doc);
        builder.withW3cDocument(dom, null);
        builder.useSVGDrawer(new BatikSVGDrawer());

        try (FileOutputStream fileOutputStream = new FileOutputStream(file.toFile())) {
            builder.toStream(fileOutputStream);
            builder.run();
        } catch (IOException e) {
            throw new GenericOnboardingException(e.getMessage());
        }

        log.debug("PDF stream properly retrieved");
    }

    @Override
    public File retrieveContractNotSigned(OnboardingWorkflow onboardingWorkflow, String productName) {
        final String onboardingId = onboardingWorkflow.getOnboarding().getId();
        final String filename =
                CONTRACT_FILENAME_FUNC.apply(onboardingWorkflow.getPdfFormatFilename(), productName);
        final String path =
                String.format("%s%s/%s", azureStorageConfig.contractPath(), onboardingId, filename);
        return azureBlobClient.getFileAsPdf(path);
    }

    @Override
    public File retrieveAttachment(OnboardingAttachment onboardingAttachment, String productName) {
        final String onboardingId = onboardingAttachment.getOnboarding().getId();
        final String filename =
                CONTRACT_FILENAME_FUNC.apply(
                        "%s_" + onboardingAttachment.getAttachment().getName() + ".pdf", productName);
        final String path =
                String.format(
                        "%s%s/%s/%s", azureStorageConfig.contractPath(), onboardingId, "attachments", filename);
        return azureBlobClient.getFileAsPdf(path);
    }

    @Override
    public Optional<File> getLogoFile() {
        if (Boolean.TRUE.equals(isLogoEnable)) {

            StringBuilder stringBuilder =
                    new StringBuilder(
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            stringBuilder.append("_").append(UUID.randomUUID()).append("_logo");
            try {
                Path path = Files.createTempFile(stringBuilder.toString(), ".png");
                Files.writeString(path, azureBlobClient.getFileAsText(logoPath));
                return Optional.of(path.toFile());
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        String.format(UNABLE_TO_DOWNLOAD_FILE.getMessage(), logoPath));
            }
        }

        return Optional.empty();
    }

    @Override
    public void uploadAggregatesCsv(OnboardingWorkflow onboardingWorkflow) {

        try {
            Onboarding onboarding = onboardingWorkflow.getOnboarding();
            Path filePath = Files.createTempFile("tempfile", ".csv");
            File csv =
                    generateAggregatesCsv(onboarding.getProductId(), onboarding.getAggregates(), filePath);
            final String path =
                    String.format(
                            "%s%s/%s",
                            azureStorageConfig.aggregatesPath(), onboarding.getId(), onboarding.getProductId());
            final String filename = "aggregates.csv";
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(csv.toPath()));
        } catch (IOException e) {
            throw new GenericOnboardingException(
                    String.format(LOAD_AGGREGATES_CSV_ERROR.getMessage(), e.getMessage()));
        }
    }

    private File generateAggregatesCsv(
            String productId, List<AggregateInstitution> institutions, Path filePath) {
        String[] headers;
        Function<AggregateInstitution, List<Object>> mapper;
        String legalSentence = null;

        // Determine headers and mapping logic based on productId
        switch (productId) {
            case "prod-io":
                headers = CSV_HEADERS_IO;
                mapper = IO_MAPPER;
                legalSentence = LEGAL_SENTENCE_IO;
                break;
            case "prod-pagopa":
                headers = CSV_HEADERS_PAGOPA;
                mapper = PAGOPA_MAPPER;
                break;
            case "prod-pn":
                headers = CSV_HEADERS_SEND;
                mapper = institution -> {
                    List<Object> records = new ArrayList<>();
                    for (User user : institution.getUsers()) {
                        UserResource userInfo = userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, user.getId());
                        records.addAll(sendMapper(userInfo, user).apply(institution));
                    }
                    return records;
                };
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Product %s is not available for aggregators", productId));
        }
        return createAggregatesCsv(institutions, filePath, headers, mapper, legalSentence);
    }

    private File createAggregatesCsv(
            List<AggregateInstitution> institutions,
            Path filePath,
            String[] headers,
            Function<AggregateInstitution, List<Object>> mapper,
            String legalSentence) {

        File csvFile = filePath.toFile();

        // Using the builder pattern to create the CSV format with headers
        CSVFormat csvFormat =
                CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader(headers).setDelimiter(';').build();

        try (FileWriter writer = new FileWriter(csvFile);
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            // Iterate over each AggregateInstitution object and write a row for each one
            for (AggregateInstitution institution : institutions) {
                csvPrinter.printRecord(mapper.apply(institution));
            }

            // Add the final legal sentence at the last row in case of prod-io
            csvPrinter.println();
            csvPrinter.printRecord(legalSentence);

        } catch (IOException e) {
            throw new GenericOnboardingException(
                    String.format(CREATE_AGGREGATES_CSV_ERROR.getMessage(), e.getMessage()));
        }
        return csvFile;
    }
}
