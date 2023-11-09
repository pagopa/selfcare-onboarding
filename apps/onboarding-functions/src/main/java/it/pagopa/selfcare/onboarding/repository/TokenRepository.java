package it.pagopa.selfcare.onboarding.repository;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import it.pagopa.selfcare.onboarding.entity.Token;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class TokenRepository implements PanacheMongoRepository<Token> {

    public Optional<Token> findByOnboardingId(String onboardingId) {
        return find("onboardingId", onboardingId).firstResultOptional();
    }
}
