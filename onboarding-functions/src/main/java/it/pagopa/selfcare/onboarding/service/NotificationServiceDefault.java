package it.pagopa.selfcare.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.config.MailTemplateConfig;
import it.pagopa.selfcare.onboarding.entity.MailTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.text.StringSubstitutor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.pagopa.selfcare.onboarding.utils.GenericError.ERROR_DURING_COMPRESS_FILE;
import static it.pagopa.selfcare.onboarding.utils.GenericError.ERROR_DURING_SEND_MAIL;


@ApplicationScoped
public class NotificationServiceDefault implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceDefault.class);

    private static final String MAIL_PARAMETER_LOG = "mailParameters: {}";
    private static final String DESTINATION_MAIL_LOG = "destinationMails: {}";

    @Inject
    MailTemplateConfig mailTemplateConfig;

    //@Inject
    Mailer mailer;
    @Inject
    AzureBlobClient azureBlobClient;
    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "onboarding-functions.sender-mail")
    String sendMail;


    @Override
    public void sendMailWithContract(String onboardingId, String filenameContract, String destination, String name, String username, String productName, String token) {
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put(mailTemplateConfig.productName(), productName);
        Optional.ofNullable(name).ifPresent(value -> mailParameters.put(mailTemplateConfig.userName(), value));
        Optional.ofNullable(username).ifPresent(value -> mailParameters.put(mailTemplateConfig.userSurname(), value));
        mailParameters.put(mailTemplateConfig.rejectTokenName(), mailTemplateConfig.rejectTokenPlaceholder() + token);
        mailParameters.put(mailTemplateConfig.confirmTokenName(), mailTemplateConfig.confirmTokenPlaceholder() + token);

        final String filepathContract = String.format("parties/docs/%s/%s", onboardingId, filenameContract);
        final String fileName = String.format("%s_accordo_adesione.pdf",productName);
        final String fileNameZip = String.format("%s_accordo_adesione.zip",productName);
        byte[] contractData = azureBlobClient.getFile(filepathContract);
        byte[] contractZipData = zipBytes(fileName, contractData);

        //List<String> destinationMail = Objects.nonNull(coreConfig.getDestinationMails()) && !coreConfig.getDestinationMails().isEmpty()
        //        ? coreConfig.getDestinationMails() : List.of(destination);
        List<String> destinationMail = List.of("manuel.rafeli@pagopa.it");

        sendMailWithFile(destinationMail, mailTemplateConfig.path(), mailParameters, contractZipData, fileNameZip, productName);
        log.debug("onboarding-contract-email Email successful sent");
    }

    public void sendMailWithFile(List<String> destinationMail, String templateName,  Map<String, String> mailParameters, byte[] fileData, String fileName, String prefixSubject) {
        try {
            log.info("START - sendMailWithFile to {}, with prefixSubject {}", destinationMail, prefixSubject);
            String template = azureBlobClient.getFileAsText(templateName);
            MailTemplate mailTemplate = objectMapper.readValue(template, MailTemplate.class);
            String html = StringSubstitutor.replace(mailTemplate.getBody(), mailParameters);
            log.trace("sendMessage start");
            
            Mail mail = Mail
                    .withHtml(destinationMail.get(0),
                            prefixSubject + ": " + mailTemplate.getSubject(),
                            html)
                    .addAttachment(fileName, fileData, "application/zip")
                    .setFrom(sendMail);

            //mailer.send(mail);

            log.info("END - sendMail to {}, with prefixSubject {}", destinationMail, prefixSubject);
        } catch (Exception e) {
            log.error(ERROR_DURING_SEND_MAIL + ":" + e.getMessage());
            throw new RuntimeException(ERROR_DURING_SEND_MAIL.getMessage());
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
