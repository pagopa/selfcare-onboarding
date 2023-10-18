package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingSaRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;

public interface OnboardingService {

    Uni<OnboardingResponse> onboarding(OnboardingDefaultRequest onboardingRequest);

    Uni<OnboardingResponse> onboardingPsp(OnboardingPspRequest onboardingRequest);

    Uni<OnboardingResponse> onboardingSa(OnboardingSaRequest onboardingRequest);

    Uni<OnboardingResponse> onboardingPa(OnboardingPaRequest onboardingRequest);
}
