package it.pagopa.selfcare.onboarding.crypto;

import it.pagopa.selfcare.onboarding.crypto.client.NamirialHttpClient;
import it.pagopa.selfcare.onboarding.crypto.entity.Credentials;
import it.pagopa.selfcare.onboarding.crypto.entity.Preferences;
import it.pagopa.selfcare.onboarding.crypto.entity.SignRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@ApplicationScoped
public class NamiralSignServiceImpl implements NamirialSignService {

    private final NamirialHttpClient namirialHttpClient;
    private final String username;
    private final String password;

    // Constructor for manual dependency injection
    public NamiralSignServiceImpl(NamirialHttpClient namirialHttpClient,
                                  @ConfigProperty(name = "onboarding-sdk-crypto.namirial.user") String username,
                                  @ConfigProperty(name = "onboarding-sdk-crypto.namirial.psw") String password
    ) {
        this.namirialHttpClient = namirialHttpClient;
        this.username = username;
        this.password = password;
    }

    @Override
    public byte[] pkcs7Signhash(InputStream is) {
        try {

            Path tempFilePath = Files.createTempFile("tempfile", ".pdf");
            File tempFile = tempFilePath.toFile();

            // Copy InputStream data to the temporary file
            Files.copy(is, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

            Credentials credentials = new Credentials(username, password);
            Preferences preferences = new Preferences("SHA256");
            SignRequest request = new SignRequest(tempFile, credentials, preferences);

            return namirialHttpClient.signDocument(request);
        } catch (IOException e) {
            throw new IllegalStateException("Something gone wrong when invoking Namirial in order to calculate pkcs7 hash sign request", e);
        }
    }
}