package it.pagopa.selfcare.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.entity.MailTemplate;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.text.StringSubstitutor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.pagopa.selfcare.onboarding.utils.GenericError.ERROR_DURING_COMPRESS_FILE;
import static it.pagopa.selfcare.onboarding.utils.GenericError.ERROR_DURING_SEND_MAIL;


@ApplicationScoped
public class NotificationServiceDefault implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceDefault.class);

    final private MailTemplatePlaceholdersConfig templatePlaceholdersConfig;
    final private MailTemplatePathConfig templatePathConfig;
    final private AzureBlobClient azureBlobClient;
    final private ObjectMapper objectMapper;
    final private ContractService contractService;
    final private String senderMail;
    final private Boolean destinationMailTest;
    final private String destinationMailTestAddress;
    final private Mailer mailer;

    public NotificationServiceDefault(MailTemplatePlaceholdersConfig templatePlaceholdersConfig, MailTemplatePathConfig templatePathConfig,
                                      AzureBlobClient azureBlobClient, ObjectMapper objectMapper, Mailer mailer, ContractService contractService,
                                      @ConfigProperty(name = "onboarding-functions.sender-mail") String senderMail,
                                      @ConfigProperty(name = "onboarding-functions.destination-mail-test") Boolean destinationMailTest,
                                      @ConfigProperty(name = "onboarding-functions.destination-mail-test-address") String destinationMailTestAddress) {
        this.templatePlaceholdersConfig = templatePlaceholdersConfig;
        this.templatePathConfig = templatePathConfig;
        this.azureBlobClient = azureBlobClient;
        this.objectMapper = objectMapper;
        this.contractService = contractService;
        this.senderMail = senderMail;
        this.destinationMailTest = destinationMailTest;
        this.destinationMailTestAddress = destinationMailTestAddress;
        this.mailer = mailer;
    }

    @Override
    public void sendMailRegistrationWithContract(String onboardingId, String destination, String name, String username, String productName, String token) {

        // Retrieve PDF contract from storage
        File contract = contractService.retrieveContractNotSigned(onboardingId);
        // Create ZIP file that contains contract
        final String fileNamePdf = String.format("%s_accordo_adesione.pdf", productName);
        final String fileNameZip = String.format("%s_accordo_adesione.zip", productName);
        byte[] contractZipData = null;

        try {
            byte[] contractData = Files.readAllBytes(contract.toPath());
            contractZipData = zipBytes(fileNamePdf, contractData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Dev mode send mail to test digital address
        List<String> destinationMail = destinationMailTest ?
                List.of(destinationMailTestAddress):
                List.of(destination);

        // Prepare data for email
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put(templatePlaceholdersConfig.productName(), productName);
        Optional.ofNullable(name).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.userName(), value));
        Optional.ofNullable(username).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.userSurname(), value));
        mailParameters.put(templatePlaceholdersConfig.rejectTokenName(), templatePlaceholdersConfig.rejectTokenPlaceholder() + token);
        mailParameters.put(templatePlaceholdersConfig.confirmTokenName(), templatePlaceholdersConfig.confirmTokenPlaceholder() + token);

        sendMailWithFile(destinationMail, templatePathConfig.registrationPath(), mailParameters, contractZipData, fileNameZip, productName);
        log.debug("Mail registration with contract successful sent !!");
    }

    public void sendMailWithFile(List<String> destinationMail, String templateName,  Map<String, String> mailParameters, byte[] fileData, String fileName, String prefixSubject) {
        try {
            log.info("START - sendMailWithFile to {}, with prefixSubject {}", destinationMail, prefixSubject);
            String template = azureBlobClient.getFileAsText(templateName);
            MailTemplate mailTemplate = objectMapper.readValue(template, MailTemplate.class);
            String html = StringSubstitutor.replace(mailTemplate.getBody(), mailParameters);
            log.trace("sendMessage start");

            final String subject = String.format("%s: %s", prefixSubject, mailTemplate.getSubject());

            Mail mail = Mail
                    .withHtml(destinationMail.get(0), subject, html)
                    .addAttachment(fileName, fileData, "application/zip")
                    .setFrom(senderMail);

            mailer.send(mail);

            log.info("END - sendMail to {}, with subject {}", destinationMail, subject);
        } catch (Exception e) {
            log.error(ERROR_DURING_SEND_MAIL + ":" + e.getMessage());
            throw new GenericOnboardingException(ERROR_DURING_SEND_MAIL.getMessage());
        }
        log.trace("sendMessage end");
    }

    public byte[] zipBytes(String filename, byte[] data)  {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(filename);

            zos.putNextEntry(entry);
            zos.write(data);
            zos.closeEntry();
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error(String.format(ERROR_DURING_COMPRESS_FILE.getMessage(), filename), e);
            throw new RuntimeException(String.format(ERROR_DURING_COMPRESS_FILE.getMessage(), filename));
        }
    }

}
