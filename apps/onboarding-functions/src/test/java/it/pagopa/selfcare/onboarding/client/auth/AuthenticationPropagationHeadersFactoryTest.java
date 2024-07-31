package it.pagopa.selfcare.onboarding.client.auth;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AuthenticationPropagationHeadersFactoryTest {

    @Inject
    AuthenticationPropagationHeadersFactory authenticationPropagationHeadersFactory;

    @Test
    void update() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        outgoingHeaders.put("user-uuid", List.of(UUID.randomUUID().toString()));
        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        assertTrue(outgoingHeaders.containsKey("Authorization"));
    }

    @Test
    void emptyHeader() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        assertTrue(outgoingHeaders.containsKey("Authorization"));
    }

}
