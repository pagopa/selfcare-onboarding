package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.onboarding.entity.Token;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TokenServiceDefault implements TokenService {

    @Override
    public Multi<Token> getToken(String onboardingId) {
        return Token.find("onboardingId", onboardingId)
                .stream();
    }
}
