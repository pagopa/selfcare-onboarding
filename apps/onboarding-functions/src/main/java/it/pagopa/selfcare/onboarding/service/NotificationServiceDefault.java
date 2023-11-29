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
import org.apache.commons.text.StringSubstitutor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.pagopa.selfcare.onboarding.utils.GenericError.ERROR_DURING_COMPRESS_FILE;
import static it.pagopa.selfcare.onboarding.utils.GenericError.ERROR_DURING_SEND_MAIL;


@ApplicationScoped
public class NotificationServiceDefault implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceDefault.class);

    private final MailTemplatePlaceholdersConfig templatePlaceholdersConfig;
    private final MailTemplatePathConfig templatePathConfig;
    private final AzureBlobClient azureBlobClient;
    private final ObjectMapper objectMapper;
    private final ContractService contractService;
    private final String senderMail;
    private final Boolean destinationMailTest;
    private final String destinationMailTestAddress;

    private final String notificationAdminMail;
    private final Mailer mailer;

    public NotificationServiceDefault(MailTemplatePlaceholdersConfig templatePlaceholdersConfig, MailTemplatePathConfig templatePathConfig,
                                      AzureBlobClient azureBlobClient, ObjectMapper objectMapper, Mailer mailer, ContractService contractService,
                                      @ConfigProperty(name = "onboarding-functions.notification-admin-email") String notificationAdminMail,
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
        this.notificationAdminMail = notificationAdminMail;
        this.mailer = mailer;
    }

    @Override
    public void sendMailRegistration(String institutionName, String destination, String name, String username, String productName) {

        // Prepare data for email
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put(templatePlaceholdersConfig.productName(), productName);
        Optional.ofNullable(name).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.notificationRequesterName(), value));
        Optional.ofNullable(username).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.notificationRequesterSurname(), value));
        mailParameters.put(templatePlaceholdersConfig.institutionDescription(), institutionName);

        sendMailWithFile(List.of(destination), templatePathConfig.registrationRequestPath(), mailParameters, productName, null);
    }

    @Override
    public void sendMailRegistrationApprove(String institutionName, String name, String username, String productName, String onboardingId) {
        sendMailOnboardingOrRegistrationApprove(institutionName, name, username, productName, onboardingId, templatePathConfig.registrationApprovePath());
    }

    @Override
    public void sendMailOnboardingApprove(String institutionName, String name, String username, String productName, String onboardingId) {
        sendMailOnboardingOrRegistrationApprove(institutionName, name, username, productName, onboardingId, templatePathConfig.onboardingApprovePath());
    }


    private void sendMailOnboardingOrRegistrationApprove(String institutionName, String name, String username, String productName, String onboardingId, String templatePath) {
        // Prepare data for email
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put(templatePlaceholdersConfig.productName(), productName);
        Optional.ofNullable(name).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.notificationRequesterName(), value));
        Optional.ofNullable(username).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.notificationRequesterSurname(), value));
        mailParameters.put(templatePlaceholdersConfig.institutionDescription(), institutionName);
        StringBuilder adminApproveLink = new StringBuilder(templatePlaceholdersConfig.adminLink());
        mailParameters.put(templatePlaceholdersConfig.confirmTokenName(), adminApproveLink.append(onboardingId).toString());

        sendMailWithFile(List.of(notificationAdminMail), templatePath, mailParameters, productName, null);
    }

    @Override
    public void sendMailRegistrationWithContract(String onboardingId, String destination, String name, String username, String productName, String token) {

        // Retrieve PDF contract from storage
        File contract = contractService.retrieveContractNotSigned(onboardingId, productName);
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

        // Prepare data for email
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put(templatePlaceholdersConfig.productName(), productName);
        Optional.ofNullable(name).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.userName(), value));
        Optional.ofNullable(username).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.userSurname(), value));
        mailParameters.put(templatePlaceholdersConfig.rejectTokenName(), templatePlaceholdersConfig.rejectTokenPlaceholder() + token);
        mailParameters.put(templatePlaceholdersConfig.confirmTokenName(), templatePlaceholdersConfig.confirmTokenPlaceholder() + token);

        FileMailData fileMailData = new FileMailData();
        fileMailData.contentType = "application/zip";
        fileMailData.data = contractZipData;
        fileMailData.name = fileNameZip;

        sendMailWithFile(List.of(destination), templatePathConfig.registrationPath(), mailParameters, productName, fileMailData);
    }

    private void sendMailWithFile(List<String> destinationMail, String templateName,  Map<String, String> mailParameters, String prefixSubject, FileMailData fileMailData) {
        try {

            // Dev mode send mail to test digital address
            List<String> destinations = destinationMailTest ?
                    List.of(destinationMailTestAddress):
                    destinationMail;

            log.info("Sending mail to {}, with prefixSubject {}", destinationMail, prefixSubject);
            String template = azureBlobClient.getFileAsText(templateName);
            MailTemplate mailTemplate = objectMapper.readValue(template, MailTemplate.class);
            String html = StringSubstitutor.replace(mailTemplate.getBody(), mailParameters);

            final String subject = String.format("%s: %s", prefixSubject, mailTemplate.getSubject());

            Mail mail = Mail
                    .withHtml(destinations.get(0), subject, html)
                    .setFrom(senderMail);

            if(Objects.nonNull(fileMailData)) {
                mail.addAttachment(fileMailData.name, fileMailData.data, fileMailData.contentType);
            }

            mailer.send(mail);

            log.info("End of sending mail to {}, with subject {}", destinationMail, subject);
        } catch (Exception e) {
            log.error(String.format("%s: %s", ERROR_DURING_SEND_MAIL, e.getMessage()));
            throw new GenericOnboardingException(ERROR_DURING_SEND_MAIL.getMessage());
        }
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

    static class FileMailData {
        byte[] data;
        String name;
        String contentType;
    }

}
