package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingSaRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;

import java.io.File;

public interface OnboardingService {

    Uni<OnboardingResponse> onboarding(OnboardingDefaultRequest onboardingRequest);

    Uni<OnboardingResponse> onboardingPsp(OnboardingPspRequest onboardingRequest);

    Uni<OnboardingResponse> onboardingSa(OnboardingSaRequest onboardingRequest);

    Uni<OnboardingResponse> onboardingPa(OnboardingPaRequest onboardingRequest);

    Uni<OnboardingResponse> onboardingConfirmation(OnboardingDefaultRequest onboardingRequest);

    Uni<OnboardingGet> approve(String onboardingId);

    Uni<Onboarding> complete(String tokenId, File contract);

    Uni<Onboarding> completeWithoutSignatureVerification(String tokenId, File contract);

    Uni<OnboardingGetResponse> onboardingGet(String productId, String taxCode, String status, String from, String to, Integer page, Integer size);

    Uni<Long> rejectOnboarding(String onboardingId);

    Uni<OnboardingGet> onboardingPending(String onboardingId);

    Uni<OnboardingGet> onboardingGet(String onboardingId);

    Uni<OnboardingGet> onboardingGetWithUserInfo(String onboardingId);
}
