package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.controller.request.AggregateInstitutionRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingImportContract;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingUserRequest;
import it.pagopa.selfcare.onboarding.controller.request.UserRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.model.Contract;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import java.util.List;

public interface OnboardingService {

    Uni<OnboardingResponse> onboarding(Onboarding onboarding, List<UserRequest> userRequests, List<AggregateInstitutionRequest> aggregates);

    Uni<OnboardingResponse> onboardingUsers(OnboardingUserRequest onboardingUserRequest, String userId);

    Uni<OnboardingResponse> onboardingImport(Onboarding onboarding, List<UserRequest> userRequests, OnboardingImportContract contractImported);

    Uni<OnboardingResponse> onboardingCompletion(Onboarding onboarding, List<UserRequest> userRequests);

    Uni<OnboardingResponse> onboardingAggregationCompletion(Onboarding onboarding, List<UserRequest> userRequests, List<AggregateInstitutionRequest> aggregates);

    Uni<OnboardingGet> approve(String onboardingId);

    Uni<Onboarding> complete(String tokenId, Contract contract);

    Uni<Onboarding> completeOnboardingUsers(String tokenId, Contract contract);

    Uni<Onboarding> completeWithoutSignatureVerification(String tokenId, Contract contract);

    Uni<OnboardingGetResponse> onboardingGet(OnboardingGetFilters filters);

    Uni<Long> rejectOnboarding(String onboardingId, String reasonForReject);

    Uni<OnboardingGet> onboardingPending(String onboardingId);

    Uni<List<OnboardingResponse>> institutionOnboardings(String taxCode, String subunitCode, String origin, String originId, OnboardingStatus status);

    Uni<List<OnboardingResponse>> verifyOnboarding(String taxCode, String subunitCode, String origin, String originId, OnboardingStatus status, String productId);

    Uni<OnboardingGet> onboardingGet(String onboardingId);

    Uni<OnboardingGet> onboardingGetWithUserInfo(String onboardingId);

    Uni<Long> updateOnboarding(String onboardingId, Onboarding onboarding);

    Uni<Boolean> checkManager(OnboardingUserRequest onboardingUserRequest);

    Uni<CustomError> checkRecipientCode(String recipientCode, String originId);

}
