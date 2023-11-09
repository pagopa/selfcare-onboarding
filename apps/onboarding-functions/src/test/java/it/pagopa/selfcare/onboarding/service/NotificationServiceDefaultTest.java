package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mailer.Mailer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@QuarkusTest
class NotificationServiceDefaultTest {

    @InjectMock
    AzureBlobClient azureBlobClient;
    @InjectMock
    ContractService contractService;
    //@InjectMock
    Mailer mailer;
    @Inject
    MailTemplatePlaceholdersConfig templatePlaceholdersConfig;

    @Inject
    NotificationServiceDefault notificationService;

    @Test
    void sendMailRegistrationWithContract() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";

        final String onboardingId = "onboardingId";
        final File file = mock(File.class);
        Mockito.when(contractService.retrieveContractNotSigned(onboardingId)).thenReturn(file);

        Mockito.when(azureBlobClient.getFileAsText(any())).thenReturn(mailTemplate);

        notificationService.sendMailRegistrationWithContract("onboardingId", "","","","","");

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFile(any());
    }

    @Test
    void sendMailRegistrationWithContract_shouldThrowException() {
        final String onboardingId = "onboardingId";
        final File file = mock(File.class);
        Mockito.when(contractService.retrieveContractNotSigned(onboardingId)).thenReturn(file);
        assertThrows(RuntimeException.class, () -> notificationService.sendMailRegistrationWithContract(onboardingId,  "example@pagopa.it","mario","rossi","prod-example","token"));
    }
}
