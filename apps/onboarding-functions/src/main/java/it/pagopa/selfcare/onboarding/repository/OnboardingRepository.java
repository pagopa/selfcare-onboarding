package it.pagopa.selfcare.onboarding.repository;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OnboardingRepository implements PanacheMongoRepository<Onboarding> {

}
