package it.pagopa.selfcare.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@QuarkusTest
class NotificationServiceDefaultTest {

    @InjectMock
    AzureBlobClient azureBlobClient;
    @InjectMock
    ContractService contractService;
    @Inject
    MailTemplatePlaceholdersConfig templatePlaceholdersConfig;
    @Inject
    MailTemplatePathConfig templatePathConfig;
    @Inject
    ObjectMapper objectMapper;
    Mailer mailer;
    NotificationServiceDefault notificationService;

    final String notificationAdminMail = "adminAddress";

    @BeforeEach
    void startup() {
        mailer = mock(Mailer.class);
        this.notificationService = new NotificationServiceDefault(templatePlaceholdersConfig, templatePathConfig,
                azureBlobClient, objectMapper, mailer, contractService, notificationAdminMail, "senderMail", false, "destinationMailTestAddress");
    }

    @Test
    void sendMailRegistrationWithContract() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";

        final String onboardingId = "onboardingId";
        final String destination = "test@test.it";
        final String productName = "productName";

        final File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("application.properties")).getFile());

        Mockito.when(contractService.retrieveContractNotSigned(onboardingId, productName))
                .thenReturn(file);

        Mockito.when(azureBlobClient.getFileAsText(templatePathConfig.registrationPath()))
                .thenReturn(mailTemplate);
        Mockito.doNothing().when(mailer).send(any());

        notificationService.sendMailRegistrationWithContract(onboardingId, destination,"","", productName);

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(destination, mailArgumentCaptor.getValue().getTo().get(0));
    }

    @Test
    void sendMailRegistrationWithContract_shouldThrowException() {
        final String onboardingId = "onboardingId";
        final String productName = "productName";
        final File file = mock(File.class);
        Mockito.when(contractService.retrieveContractNotSigned(onboardingId, productName)).thenReturn(file);
        assertThrows(RuntimeException.class, () -> notificationService.sendMailRegistrationWithContract(onboardingId,  "example@pagopa.it","mario","rossi","prod-example"));
    }

    @Test
    void sendMailRegistration() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";

        final String onboardingId = "onboardingId";
        final String destination = "test@test.it";

        Mockito.when(azureBlobClient.getFileAsText(templatePathConfig.registrationRequestPath()))
                .thenReturn(mailTemplate);

        Mockito.doNothing().when(mailer).send(any());

        notificationService.sendMailRegistration(onboardingId, destination,"","","");

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(destination, mailArgumentCaptor.getValue().getTo().get(0));
    }

    @Test
    void sendCompletedEmail() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";
        final String institutionName = "institutionName";
        final String destination = "test@test.it";
        Product product = new Product();
        product.setTitle("productName");
        product.setId("prod-id");

        final File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("application.properties")).getFile());

        Mockito.when(contractService.getLogoFile()).thenReturn(Optional.of(file));

        Mockito.when(azureBlobClient.getFileAsText(templatePathConfig.registrationPath()))
                .thenReturn(mailTemplate);
        Mockito.doNothing().when(mailer).send(any());

        notificationService.sendCompletedEmail(institutionName, List.of(destination), product, InstitutionType.PA);

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(destination, mailArgumentCaptor.getValue().getTo().get(0));
    }

    @Test
    void sendMailRejection() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";

        final String destination = "test@test.it";
        Product product = new Product();
        product.setTitle("productName");
        product.setId("prod-id");

        final File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("application.properties")).getFile());

        Mockito.when(contractService.getLogoFile()).thenReturn(Optional.of(file));

        Mockito.when(azureBlobClient.getFileAsText(templatePathConfig.rejectPath()))
                .thenReturn(mailTemplate);
        Mockito.doNothing().when(mailer).send(any());

        notificationService.sendMailRejection(List.of(destination), product);

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(destination, mailArgumentCaptor.getValue().getTo().get(0));
    }


    @Test
    void sendMailRegistrationApprove() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";
        final String institutionName = "institutionName";

        Mockito.when(azureBlobClient.getFileAsText(templatePathConfig.registrationApprovePath()))
                .thenReturn(mailTemplate);
        Mockito.doNothing().when(mailer).send(any());

        notificationService.sendMailRegistrationApprove(institutionName, "name","username","product","token");

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(notificationAdminMail, mailArgumentCaptor.getValue().getTo().get(0));
    }


    @Test
    void sendMailOnboardingApprove() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";
        final String institutionName = "institutionName";

        Mockito.when(azureBlobClient.getFileAsText(templatePathConfig.onboardingApprovePath()))
                .thenReturn(mailTemplate);
        Mockito.doNothing().when(mailer).send(any());

        notificationService.sendMailOnboardingApprove(institutionName, "name","username","product","token");

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(notificationAdminMail, mailArgumentCaptor.getValue().getTo().get(0));
    }

}
