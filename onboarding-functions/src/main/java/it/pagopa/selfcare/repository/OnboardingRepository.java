package it.pagopa.selfcare.repository;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import it.pagopa.selfcare.entity.Onboarding;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OnboardingRepository implements PanacheMongoRepository<Onboarding> {

}
