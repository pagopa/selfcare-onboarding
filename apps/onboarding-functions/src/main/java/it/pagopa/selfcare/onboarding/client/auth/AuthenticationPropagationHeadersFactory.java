package it.pagopa.selfcare.onboarding.client.auth;

import it.pagopa.selfcare.onboarding.service.JwtSessionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;


@ApplicationScoped
public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    @Inject
    JwtSessionService tokenService;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        String bearerToken;
        if (!clientOutgoingHeaders.isEmpty() && clientOutgoingHeaders.containsKey("user-uuid")) {
            final String uuid = clientOutgoingHeaders.get("user-uuid").get(0);
            bearerToken = tokenService.createJwt(uuid);
        } else {
            bearerToken = System.getenv("JWT_BEARER_TOKEN");
        }
        clientOutgoingHeaders.put("Authorization", List.of("Bearer " + bearerToken));
        return clientOutgoingHeaders;
    }
}