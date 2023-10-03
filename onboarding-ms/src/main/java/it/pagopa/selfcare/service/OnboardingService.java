package it.pagopa.selfcare.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.controller.request.OnboardingSaRequest;
import it.pagopa.selfcare.controller.response.OnboardingResponse;
import it.pagopa.selfcare.entity.Onboarding;

public interface OnboardingService {

    Uni<OnboardingResponse> onboarding(OnboardingDefaultRequest onboardingRequest);

    Uni<OnboardingResponse> onboardingPsp(OnboardingPspRequest onboardingRequest);

    Uni<OnboardingResponse> onboardingSa(OnboardingSaRequest onboardingRequest);

    Uni<OnboardingResponse> onboardingPa(OnboardingPaRequest onboardingRequest);
}
