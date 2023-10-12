package it.pagopa.selfcare.service;

import io.quarkus.mailer.Mailer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.config.MailTemplateConfig;
import it.pagopa.selfcare.exception.GenericOnboardingException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
public class NotificationServiceDefaultTest {

    @InjectMock
    AzureBlobClient azureBlobClient;
    //@InjectMock
    Mailer mailer;

    @Inject
    NotificationServiceDefault notificationService;

    @Test
    void sendMailWithContract() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";

        Mockito.when(azureBlobClient.getFile(any())).thenReturn("example".getBytes(StandardCharsets.UTF_8));

        Mockito.when(azureBlobClient.getFileAsText(any())).thenReturn(mailTemplate);
        //Mockito.doNothing().when(mailer).send(any());

        notificationService.sendMailWithContract("onboardingId", "filenameContract", "","","","","");
    }

    @Test
    void sendMailWithContract_shouldThrowException() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";

        Mockito.when(azureBlobClient.getFile(any())).thenReturn("example".getBytes(StandardCharsets.UTF_8));

        Mockito.when(azureBlobClient.getFileAsText(any())).thenThrow(new IllegalArgumentException());
        //Mockito.doNothing().when(mailer).send(any());

        assertThrows(RuntimeException.class, () -> notificationService.sendMailWithContract("onboardingId", "filenameContract", "","","","",""));
    }
}
