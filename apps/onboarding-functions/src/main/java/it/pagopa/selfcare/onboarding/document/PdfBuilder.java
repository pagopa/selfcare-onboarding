package it.pagopa.selfcare.onboarding.document;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import it.pagopa.selfcare.onboarding.exception.PdfBuilderException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static it.pagopa.selfcare.onboarding.utils.GenericError.PDF_CREATION_FAILED;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PdfBuilder {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String CLASSPATH_BASE_URI = "classpath:/";

    public static File generateDocument(String documentName,
                                        String documentTemplate,
                                        Map<String, Object> content) {
        Path temporaryPdfFile = null;
        Path temporaryDirectory = null;

        try {
            String nameFile = DATE_TIME_FORMATTER.format(LocalDateTime.now())
                    + "_" + UUID.randomUUID()
                    + "_" + documentName;

            FileAttribute<Set<PosixFilePermission>> dirAttr =
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));

            temporaryDirectory = Files.createTempDirectory("pdfgen-", dirAttr);

            FileAttribute<Set<PosixFilePermission>> fileAttr =
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));

            temporaryPdfFile = Files.createTempFile(temporaryDirectory, nameFile, ".pdf", fileAttr);

            String htmlContent = StringSubstitutor.replace(documentTemplate, content);

            Document dom = new W3CDom().fromJsoup(Jsoup.parse(htmlContent));

            PdfRendererBuilder builder = buildRenderer(dom);

            try (FileOutputStream out = new FileOutputStream(temporaryPdfFile.toFile())) {
                builder.toStream(out);
                builder.run();
            }

            return temporaryPdfFile.toFile();

        } catch (Exception e) {
            log.error("Error while generating PDF", e);
            if (temporaryPdfFile != null) {
                try { Files.deleteIfExists(temporaryPdfFile); } catch (Exception ignored) {}
            }
            if (temporaryDirectory != null) {
                try { Files.deleteIfExists(temporaryDirectory); } catch (Exception ignored) {}
            }
            throw new PdfBuilderException(PDF_CREATION_FAILED.getMessage(), PDF_CREATION_FAILED.getCode());
        }
    }

    private static PdfRendererBuilder buildRenderer(Document dom) {
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useSVGDrawer(new BatikSVGDrawer());
        builder.withW3cDocument(dom, CLASSPATH_BASE_URI);
        return builder;
    }

}

