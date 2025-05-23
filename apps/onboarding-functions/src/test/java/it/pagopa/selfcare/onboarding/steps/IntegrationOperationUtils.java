package it.pagopa.selfcare.onboarding.steps;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import jakarta.inject.Inject;

@QuarkusTest
public class IntegrationOperationUtils {

  @Inject OnboardingRepository onboardingRepository;

  @Inject TokenRepository tokenRepository;

  public <T> void persistIntoMongo(T input) {
    if (input instanceof Token) {
      tokenRepository.persist((Token) input);
    } else {
      onboardingRepository.persist((Onboarding) input);
    }
  }

  public Onboarding findIntoMongoOnboarding(String id) {
    return onboardingRepository.findById(id);
  }

  public Token findIntoMongoToken(String id) {
    return tokenRepository.findById(id);
  }
}
