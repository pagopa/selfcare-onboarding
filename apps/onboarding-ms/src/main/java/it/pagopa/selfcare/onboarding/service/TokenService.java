package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Token;
import jakarta.ws.rs.core.Response;

import java.util.List;

public interface TokenService {
    Uni<List<Token>> getToken(String onboardingId);

    Uni<Response> retrieveContractNotSigned(String onboardingId);
}
