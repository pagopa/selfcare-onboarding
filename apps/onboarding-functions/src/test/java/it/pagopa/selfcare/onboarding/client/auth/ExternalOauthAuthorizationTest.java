package it.pagopa.selfcare.onboarding.client.auth;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.client.external.ExternalTokenRestClient;
import it.pagopa.selfcare.onboarding.dto.OauthToken;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class ExternalOauthAuthorizationTest {
    @Inject
    ExternalOauthAuthorization externalOauthAuthorization;

    @InjectMock
    @RestClient
    ExternalTokenRestClient tokenRestClient;

    @Test
    void filter() throws IOException {
        ClientRequestContext clientRequest = mock(ClientRequestContext.class);
        when(clientRequest.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        OauthToken oauthToken = new OauthToken();
        oauthToken.setAccessToken("test");
        when(tokenRestClient.getToken(any())).thenReturn(oauthToken);
        externalOauthAuthorization.filter(clientRequest);
        assertFalse(clientRequest.getHeaders().isEmpty());
    }
}