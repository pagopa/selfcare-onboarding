package it.pagopa.selfcare.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.entity.Onboarding;

public interface OnboardingService {

    Uni<Onboarding> onboarding(Onboarding onboardingRequest);
}
