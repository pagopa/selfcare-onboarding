package it.pagopa.selfcare.onboarding.client.auth;

import it.pagopa.selfcare.onboarding.service.TokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;


@ApplicationScoped
public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    @Inject
    TokenService tokenService;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        final String uuid = incomingHeaders.get("user-uuid").get(0);
        String bearerToken = tokenService.createJwt(uuid);
        clientOutgoingHeaders.put("Authorization", List.of("Bearer " + bearerToken));
        return clientOutgoingHeaders;
    }
}