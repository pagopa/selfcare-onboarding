package it.pagopa.selfcare.onboarding.client.auth;

import it.pagopa.selfcare.onboarding.client.fd.FDTokenRestClient;
import it.pagopa.selfcare.onboarding.config.FDConfig;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Form;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FDOauthAuthorization implements ClientRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(FDOauthAuthorization.class);
    private final FDConfig fdConfig;
    private final FDTokenRestClient tokenRestClient;

    public FDOauthAuthorization(@Context FDConfig fdConfig,
                                @Context @RestClient FDTokenRestClient tokenRestClient) {
        this.fdConfig = fdConfig;
        this.tokenRestClient = tokenRestClient;
    }
    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        String accessToken = tokenRestClient.getFDToken(buildFDTokenEntity()).getAccessToken();
        log.info("Token retrieved successfully");
        clientRequestContext.getHeaders().add("Authorization", "Bearer " + accessToken);
    }

    private Form buildFDTokenEntity() {
        Form form = new Form();
        form.param("grant_type", fdConfig.grantType());
        form.param("client_id", fdConfig.clientId());
        form.param("client_secret", fdConfig.clientSecret());
        return form;
    }
}
