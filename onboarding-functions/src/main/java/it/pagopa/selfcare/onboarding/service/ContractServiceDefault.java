package it.pagopa.selfcare.onboarding.service;

import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.FSStreamFactory;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.commons.base.utils.InstitutionType;
import it.pagopa.selfcare.onboarding.config.AzureStorageConfig;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.utils.ClassPathStream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.text.StringSubstitutor;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.selfcare.onboarding.common.ProductId.*;
import static it.pagopa.selfcare.onboarding.utils.GenericError.GENERIC_ERROR;
import static it.pagopa.selfcare.onboarding.utils.PdfMapper.*;

@ApplicationScoped
public class ContractServiceDefault implements ContractService {


    private static final Logger log = LoggerFactory.getLogger(ContractServiceDefault.class);
    public static final String PDF_FORMAT_FILENAME = "%s.pdf";

    @Inject
    AzureStorageConfig azureStorageConfig;

    @Inject
    AzureBlobClient azureBlobClient;

    @Override
    public File createContractPDF(String contractTemplatePath, Onboarding onboarding, UserResource validManager, List<UserResource> users, List<String> geographicTaxonomies) {

        log.info("START - createContractPdf for template: {}", contractTemplatePath);

        final String builder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_" + UUID.randomUUID() + "_contratto_interoperabilita.";
        final String productId = onboarding.getProductId();
        final Institution institution = onboarding.getInstitution();

        try {
            String contractTemplateText = azureBlobClient.getFileAsText(contractTemplatePath);

            Path files = Files.createTempFile(builder, ".pdf");
            Map<String, Object> data = setUpCommonData(validManager, users, institution, onboarding.getBilling(), List.of());
            if (PROD_PAGOPA.getValue().equalsIgnoreCase(productId) &&
                    InstitutionType.PSP == institution.getInstitutionType()) {
                setupPSPData(data, validManager, institution);
            } else if (PROD_IO.getValue().equalsIgnoreCase(productId)
                    || PROD_IO_PREMIUM.getValue().equalsIgnoreCase(productId)
                    || PROD_IO_SIGN.getValue().equalsIgnoreCase(productId)) {
                setupProdIOData(onboarding, data, validManager);
            } else if (PROD_PN.getValue().equalsIgnoreCase(productId)){
                setupProdPNData(data, institution, onboarding.getBilling());
            } else if (PROD_INTEROP.getValue().equalsIgnoreCase(productId)){
                setupSAProdInteropData(data, institution);
            }
            log.debug("data Map for PDF: {}", data);
            getPDFAsFile(files, contractTemplateText, data);

            //return signContract(institution, request, files.toFile());
            final String filename = String.format(PDF_FORMAT_FILENAME, onboarding.getOnboardingId());
            final String path = String.format("%s%s", azureStorageConfig.contractPath(), onboarding.getOnboardingId());
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(files));

            return files.toFile();
        } catch (IOException e) {
            throw new GenericOnboardingException(String.format("Can not create contract PDF, message: %s", e.getMessage()));
        }
    }

    @Override
    public File loadContractPDF(String contractTemplatePath, String onboardingId) {
        try {
            File pdf = azureBlobClient.getFileAsPdf(contractTemplatePath);

            final String filename = String.format(PDF_FORMAT_FILENAME, onboardingId);
            final String path = String.format("%s/%s", azureStorageConfig.contractPath(), onboardingId);
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(pdf.toPath()));

            return pdf;
        } catch (IOException e) {
            throw new GenericOnboardingException(String.format("Can not load contract PDF, message: %s", e.getMessage()));
        }
    }

        private void getPDFAsFile(Path files, String contractTemplate, Map<String, Object> data) {
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

        try(FileOutputStream fileOutputStream = new FileOutputStream(files.toFile())) {
            builder.toStream(fileOutputStream);
            builder.run();
        } catch (IOException e){
            throw new GenericOnboardingException(e.getMessage());
        }

        log.debug("PDF stream properly retrieved");
    }

    @Override
    public File retrieveContractNotSigned(String onboardingId) {
        final String filename = String.format(PDF_FORMAT_FILENAME, onboardingId);
        final String path = String.format("%s%s/%s", azureStorageConfig.contractPath(), onboardingId, filename);
        return azureBlobClient.getFileAsPdf(path);
    }

}
