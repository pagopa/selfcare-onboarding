package it.pagopa.selfcare.onboarding.client.auth;

import it.pagopa.selfcare.onboarding.client.external.ExternalTokenRestClient;
import it.pagopa.selfcare.onboarding.config.ExternalConfig;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Form;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ExternalOauthAuthorization implements ClientRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ExternalOauthAuthorization.class);
    private final ExternalConfig externalConfig;
    private final ExternalTokenRestClient tokenRestClient;

    public ExternalOauthAuthorization(@Context ExternalConfig externalConfig,
                                      @Context @RestClient ExternalTokenRestClient tokenRestClient) {
        this.externalConfig = externalConfig;
        this.tokenRestClient = tokenRestClient;
    }
    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        String accessToken = tokenRestClient.getToken(buildTokenEntity()).getAccessToken();
        log.info("Token retrieved successfully");
        clientRequestContext.getHeaders().add("Authorization", "Bearer " + accessToken);
    }

    private Form buildTokenEntity() {
        Form form = new Form();
        form.param("grant_type", externalConfig.grantType());
        form.param("client_id", externalConfig.clientId());
        form.param("client_secret", externalConfig.clientSecret());
        return form;
    }
}
