package it.pagopa.selfcare.onboarding.client.auth;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class EventhubSasTokenAuthorization implements ClientRequestFilter {

    private final URI resourceUri;
    private final String keyName;
    private final String key;

    public EventhubSasTokenAuthorization(@ConfigProperty(name = "rest-client.event-hub.uri") URI resourceUri,
                                         @ConfigProperty(name = "eventhub.rest-client.keyName") String keyName,
                                         @ConfigProperty(name = "eventhub.rest-client.key") String key) {
        this.resourceUri = resourceUri;
        this.keyName = keyName;
        this.key = key;
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext) {
        final String[] segments = clientRequestContext.getUri().getPath().split("/");
        final String topic = segments[segments.length - 1];
        clientRequestContext.getHeaders().add("Authorization", getSASToken(resourceUri.toString(), keyName, key));
    }

    private static String getSASToken(String resourceUri, String keyName, String key) {
        final long epoch = System.currentTimeMillis() / 1000L;
        final int week = 60 * 60 * 24 * 7;
        String expiry = Long.toString(epoch + week);

        String sasToken;
        final String stringToSign = URLEncoder.encode(resourceUri, StandardCharsets.UTF_8) + "\n" + expiry;
        final String signature = getHMAC256(key, stringToSign);
        sasToken = "SharedAccessSignature sr=" + URLEncoder.encode(resourceUri, StandardCharsets.UTF_8) + "&sig=" +
                URLEncoder.encode(signature, StandardCharsets.UTF_8) + "&se=" + expiry + "&skn=" + keyName;

        return sasToken;
    }

    private static String getHMAC256(String key, String input) {
        Mac sha256_HMAC;
        String hash = null;
        try {
            sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            Base64.Encoder encoder = Base64.getEncoder();

            hash = new String(encoder.encode(sha256_HMAC.doFinal(input.getBytes(StandardCharsets.UTF_8))));

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return hash;
    }
}
