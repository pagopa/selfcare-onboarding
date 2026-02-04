package it.pagopa.selfcare.onboarding.document;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.utils.ClassPathStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.selfcare.onboarding.utils.GenericError.GENERIC_ERROR;

@Slf4j
@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class PdfBuilder {

    private static final String DATE_PATTERN_YYYY_M_MDD_H_HMMSS = "yyyyMMddHHmmss";

    public static File generateDocument(String documentName, String documentTemplate, Map<String, Object> content) throws IOException {

        final String nameFile = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN_YYYY_M_MDD_H_HMMSS)) +
                "_" + UUID.randomUUID() + documentName;

        Path temporaryPdfFile = generateFile(nameFile);

        if (Objects.nonNull(temporaryPdfFile)) {
            log.debug("Getting PDF for HTML template...");
            String htmlContent = StringSubstitutor.replace(documentTemplate, content);

            Document dom = W3CDom.convert(Jsoup.parse(htmlContent, "UTF-8"));
            PdfRendererBuilder builder = getPdfRendererBuilder(dom);

            buildOutputStream(temporaryPdfFile, builder);

            log.debug("PDF stream properly retrieved");
            return temporaryPdfFile.toFile();

        } else {
            throw new IOException("Invalid file path");
        }

    }

    private static Path generateFile(String nameFile) {
        // Create a temporary PDF file to store the contract.
        try {
            return Files.createTempFile(nameFile, ".pdf");
        } catch (IOException e) {
            log.error("Unable to create file ", e);
            return null;
        }
    }

    private static PdfRendererBuilder getPdfRendererBuilder(Document dom) {
        // Create a pdfRenderedBuilder instance
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useProtocolsStreamImplementation(url -> {
            URI fullUri;
            try {
                fullUri = new URI(url);
                return new ClassPathStream(fullUri.getPath());
            } catch (URISyntaxException e) {
                log.error("URISintaxException in ClassPathStreamFactory: ", e);
                throw new GenericOnboardingException(GENERIC_ERROR.getMessage(), GENERIC_ERROR.getCode());
            }
        }, "classpath");

        builder.useSVGDrawer(new BatikSVGDrawer());
        builder.withW3cDocument(dom, null);

        return builder;
    }

    private static void buildOutputStream(Path temporaryPdfFile, PdfRendererBuilder builder) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(temporaryPdfFile.toFile())) {
            builder.toStream(fileOutputStream);
            builder.run();
        } catch (IOException e) {
            throw new GenericOnboardingException(e.getMessage());
        }
    }

}
