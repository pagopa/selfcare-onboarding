package it.pagopa.selfcare.service;

import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.FSStreamFactory;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.commons.base.utils.InstitutionType;
import it.pagopa.selfcare.config.AzureStorageConfig;
import it.pagopa.selfcare.entity.Institution;
import it.pagopa.selfcare.entity.Onboarding;
import it.pagopa.selfcare.exception.GenericOnboardingException;
import it.pagopa.selfcare.utils.ClassPathStream;
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
import static it.pagopa.selfcare.utils.GenericError.GENERIC_ERROR;
import static it.pagopa.selfcare.utils.PdfMapper.*;

@ApplicationScoped
public class ContractServiceDefault implements ContractService {


    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

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
            final String filename = String.format("%s.pdf", onboarding.getId());
            final String path = String.format("%s7%s", azureStorageConfig.contractPath(), onboarding.getId().toHexString());
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(files));

            return files.toFile();
        } catch (IOException e) {
            log.warn("can not create contract PDF", e);
            throw new GenericOnboardingException(e.getMessage(), "0000");
        }
    }

    private void getPDFAsFile(Path files, String contractTemplate, Map<String, Object> data) {
        log.debug("Getting PDF for HTML template...");
        String html = StringSubstitutor.replace(contractTemplate, data);
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useProtocolsStreamImplementation(new FSStreamFactory() {
            @Override
            public FSStream getUrl(String url) {
                URI fullUri;
                try {
                    fullUri = new URI(url);
                    return new ClassPathStream(fullUri.getPath());
                } catch (URISyntaxException e) {
                    log.error("URISintaxException in ClassPathStreamFactory: ",e);
                    throw new GenericOnboardingException(GENERIC_ERROR.getMessage(), GENERIC_ERROR.getCode());
                }
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
            throw new RuntimeException(e);
        }

        log.debug("PDF stream properly retrieved");
    }
}
