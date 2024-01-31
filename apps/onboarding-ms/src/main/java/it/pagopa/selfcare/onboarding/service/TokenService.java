package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.onboarding.entity.Token;

public interface TokenService {
    Multi<Token> getToken(String onboardingId);
}
