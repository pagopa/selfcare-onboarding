package it.pagopa.selfcare.onboarding.repository;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OnboardingRepository implements PanacheMongoRepositoryBase<Onboarding, String> {

}
