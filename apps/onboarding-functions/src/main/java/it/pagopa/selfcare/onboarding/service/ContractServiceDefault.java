package it.pagopa.selfcare.onboarding.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.config.AzureStorageConfig;
import it.pagopa.selfcare.onboarding.config.PagoPaSignatureConfig;
import it.pagopa.selfcare.onboarding.crypto.PadesSignService;
import it.pagopa.selfcare.onboarding.crypto.entity.SignatureInformation;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.utils.ClassPathStream;
import it.pagopa.selfcare.onboarding.utils.GenericError;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static it.pagopa.selfcare.onboarding.common.ProductId.*;
import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_WORKS_FIELD_LIST;
import static it.pagopa.selfcare.onboarding.utils.GenericError.GENERIC_ERROR;
import static it.pagopa.selfcare.onboarding.utils.GenericError.UNABLE_TO_DOWNLOAD_FILE;
import static it.pagopa.selfcare.onboarding.utils.PdfMapper.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.CONTRACT_FILENAME_FUNC;

@ApplicationScoped
public class ContractServiceDefault implements ContractService {

    @RestClient
    @Inject
    UserApi userRegistryApi;
    private static final Logger log = LoggerFactory.getLogger(ContractServiceDefault.class);
    public static final String PAGOPA_SIGNATURE_DISABLED = "disabled";
    private final AzureStorageConfig azureStorageConfig;
    private final AzureBlobClient azureBlobClient;
    private final PadesSignService padesSignService;
    private final PagoPaSignatureConfig pagoPaSignatureConfig;
    private final OnboardingRepository onboardingRepository;
    Boolean isLogoEnable;
    private final String logoPath;

    public ContractServiceDefault(AzureStorageConfig azureStorageConfig,
                                  AzureBlobClient azureBlobClient, PadesSignService padesSignService,
                                  PagoPaSignatureConfig pagoPaSignatureConfig,
                                  @ConfigProperty(name = "onboarding-functions.logo-path") String logoPath,
                                  @ConfigProperty(name = "onboarding-functions.logo-enable") Boolean isLogoEnable,
                                  OnboardingRepository onboardingRepository) {
        this.azureStorageConfig = azureStorageConfig;
        this.azureBlobClient = azureBlobClient;
        this.padesSignService = padesSignService;
        this.pagoPaSignatureConfig = pagoPaSignatureConfig;
        this.logoPath = logoPath;
        this.isLogoEnable = isLogoEnable;
        this.onboardingRepository = onboardingRepository;
    }

    /**
     * Creates a PDF contract document from a given contract template file and institution data.
     * Based on @contractTemplatePath it loads contract template as test and replace placeholder using a map <key,value> with institution information.
     * Contract will be stored at parties/docs/{onboardingId}/{productName}_accordo_di_adesione.pdf
     *
     * @param contractTemplatePath   The file path to the contract template.
     * @param onboarding             Information related to the onboarding process.
     * @param manager           A user resource representing a valid manager.
     * @param users                  A list of user resources.
     * @param productName   Product's name of onboarding.
     * @return                       A File object representing the created PDF contract document.
     * @throws GenericOnboardingException If an error occurs during PDF generation.
     */
    @Override
    public File createContractPDF(String contractTemplatePath, Onboarding onboarding, UserResource manager, List<UserResource> users, String productName) {

        log.info("START - createContractPdf for template: {}", contractTemplatePath);
        // Generate a unique filename for the PDF.
        final String productId = onboarding.getProductId();
        final Institution institution = onboarding.getInstitution();

        try {
            final String[] split = contractTemplatePath.split("\\.");
            final String fileType = split[split.length-1];

            // If contract template is a PDF, I get without parsing
            File temporaryPdfFile = "pdf".equals(fileType)
                    ? azureBlobClient.getFileAsPdf(contractTemplatePath)
                    : createPdfFileContract(contractTemplatePath, onboarding, manager, users);

            // Define the filename and path for storage.
            final String filename = CONTRACT_FILENAME_FUNC.apply(productName);
            final String path = String.format("%s%s", azureStorageConfig.contractPath(), onboarding.getId());

            File signedPath = signPdf(temporaryPdfFile, institution.getDescription(), productId);
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(signedPath.toPath()));

            return signedPath;
        } catch (IOException e) {
            throw new GenericOnboardingException(String.format("Can not create contract PDF, message: %s", e.getMessage()));
        }
    }

    private File createPdfFileContract(String contractTemplatePath, Onboarding onboarding, UserResource manager, List<UserResource> users) throws IOException {
        final String builder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                "_" + UUID.randomUUID() + "_contratto_interoperabilita.";

        final String productId = onboarding.getProductId();
        final Institution institution = onboarding.getInstitution();

        // Read the content of the contract template file.
        String contractTemplateText = azureBlobClient.getFileAsText(contractTemplatePath);
        // Create a temporary PDF file to store the contract.
        Path temporaryPdfFile = Files.createTempFile(builder, ".pdf");
        // Prepare common data for the contract document.
        Map<String, Object> data = setUpCommonData(manager, users, onboarding);
        // Set data of previous manager in case of users onboarding
        if (Objects.nonNull(onboarding.getReferenceOnboardingId())) {
            setPreviousManagerData(onboarding, manager, data);
        }
        // Customize data based on the product and institution type.
        if (PROD_PAGOPA.getValue().equalsIgnoreCase(productId) &&
                InstitutionType.PSP == institution.getInstitutionType()) {
            setupPSPData(data, manager, onboarding);
        } else if(PROD_PAGOPA.getValue().equalsIgnoreCase(productId) &&
                InstitutionType.PSP != institution.getInstitutionType()
                && InstitutionType.PT != institution.getInstitutionType()) {
            setECData(data, onboarding);
        } else if (PROD_IO.getValue().equalsIgnoreCase(productId)
                || PROD_IO_PREMIUM.getValue().equalsIgnoreCase(productId)
                || PROD_IO_SIGN.getValue().equalsIgnoreCase(productId)) {
            setupProdIOData(onboarding, data, manager);
        } else if (PROD_PN.getValue().equalsIgnoreCase(productId)){
            setupProdPNData(data, institution, onboarding.getBilling());
        } else if (PROD_INTEROP.getValue().equalsIgnoreCase(productId)){
            setupSAProdInteropData(data, institution);
        }
        log.debug("data Map for PDF: {}", data);
        fillPDFAsFile(temporaryPdfFile, contractTemplateText, data);
        return temporaryPdfFile.toFile();
    }

    private void setPreviousManagerData(Onboarding onboarding, UserResource manager, Map<String, Object> data) {
        onboardingRepository.findByIdOptional(onboarding.getReferenceOnboardingId())
                .ifPresent(previousOnboarding -> {
                    final String previousManagerId =  previousOnboarding.getUsers().stream()
                            .filter(user -> PartyRole.MANAGER == user.getRole())
                            .map(User::getId)
                            .findAny()
                            .orElseThrow(() -> new GenericOnboardingException(
                                    GenericError.MANAGER_NOT_FOUND_GENERIC_ERROR.getMessage(),
                                    GenericError.MANAGER_NOT_FOUND_GENERIC_ERROR.getCode())
                            );
                    UserResource previousManager = userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, previousManagerId);
                    if (!previousManager.getId().equals(manager.getId())) {
                        data.put("previousManagerName", previousManager.getName().getValue());
                        data.put("previousManagerSurname",previousManager.getFamilyName().getValue());
                    }
                });
    }

    private File signPdf(File pdf, String institutionDescription, String productId) throws IOException {
        if(PAGOPA_SIGNATURE_DISABLED.equals(pagoPaSignatureConfig.source())) {
            log.info("Skipping PagoPA contract pdf sign due to global disabling");
            return pdf;
        }

        String signReason = pagoPaSignatureConfig.applyOnboardingTemplateReason()
                .replace("${institutionName}", institutionDescription)
                .replace("${productName}", productId);

        log.info("Signing input file {} using reason {}", pdf.getName(), signReason);
        Path signedPdf = Files.createTempFile("signed", ".pdf");
        padesSignService.padesSign(pdf, signedPdf.toFile(), buildSignatureInfo(signReason));
        return signedPdf.toFile();
    }

    private SignatureInformation buildSignatureInfo(String signReason) {
        return new SignatureInformation(
                pagoPaSignatureConfig.signer(),
                pagoPaSignatureConfig.location(),
                signReason
        );
    }

    @Override
    public File loadContractPDF(String contractTemplatePath, String onboardingId, String productName) {
        try {
            File pdf = azureBlobClient.getFileAsPdf(contractTemplatePath);

            final String filename = CONTRACT_FILENAME_FUNC.apply(productName);
            final String path = String.format("%s/%s", azureStorageConfig.contractPath(), onboardingId);
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(pdf.toPath()));

            return pdf;
        } catch (IOException e) {
            throw new GenericOnboardingException(String.format("Can not load contract PDF, message: %s", e.getMessage()));
        }
    }

    private void fillPDFAsFile(Path file, String contractTemplate, Map<String, Object> data) {
        log.debug("Getting PDF for HTML template...");
        String html = StringSubstitutor.replace(contractTemplate, data);
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useProtocolsStreamImplementation(url -> {
            URI fullUri;
            try {
                fullUri = new URI(url);
                return new ClassPathStream(fullUri.getPath());
            } catch (URISyntaxException e) {
                log.error("URISintaxException in ClassPathStreamFactory: ",e);
                throw new GenericOnboardingException(GENERIC_ERROR.getMessage(), GENERIC_ERROR.getCode());
            }
        }, "classpath");
        var doc = Jsoup.parse(html, "UTF-8");
        var dom = W3CDom.convert(doc);
        builder.withW3cDocument(dom, null);
        builder.useSVGDrawer(new BatikSVGDrawer());

        try(FileOutputStream fileOutputStream = new FileOutputStream(file.toFile())) {
            builder.toStream(fileOutputStream);
            builder.run();
        } catch (IOException e){
            throw new GenericOnboardingException(e.getMessage());
        }

        log.debug("PDF stream properly retrieved");
    }

    @Override
    public File retrieveContractNotSigned(String onboardingId, String productName) {
        final String filename = CONTRACT_FILENAME_FUNC.apply(productName);
        final String path = String.format("%s%s/%s", azureStorageConfig.contractPath(), onboardingId, filename);
        return azureBlobClient.getFileAsPdf(path);
    }

    @Override
    public Optional<File> getLogoFile() {
        if (Objects.nonNull(isLogoEnable) && isLogoEnable) {

            StringBuilder stringBuilder = new StringBuilder(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            stringBuilder.append("_").append(UUID.randomUUID()).append("_logo");
            try {
                Path path = Files.createTempFile(stringBuilder.toString(), ".png");
                Files.writeString(path, azureBlobClient.getFileAsText(logoPath));
                return Optional.of(path.toFile());
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format(UNABLE_TO_DOWNLOAD_FILE.getMessage(), logoPath));
            }
        }

        return Optional.empty();
    }
}
