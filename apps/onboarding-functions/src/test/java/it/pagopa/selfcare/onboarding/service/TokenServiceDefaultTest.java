package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(TokenServiceDefaultTest.TokenProfile.class)
class TokenServiceDefaultTest {

    @Inject
    TokenServiceDefault tokenService;

    public static class TokenProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            InputStream is = getClass().getClassLoader().getResourceAsStream("certs/PKCS1Key.pem");
            String privateKey = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));
            return Map.of("onboarding-functions.jwt.token.signing-key", privateKey,
                    "onboarding-functions.jwt.token.issuer", "https://dev.selfcare.pagopa.it"
            );
        }
    }

    @Test
    void createJwt() {
        final String userId = "userId";
        String jwt = tokenService.createJwt(userId);
        assertTrue(Objects.nonNull(jwt));
    }

}
