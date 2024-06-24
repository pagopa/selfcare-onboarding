package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;

public interface CompletionService {

    String createInstitutionAndPersistInstitutionId(Onboarding onboarding);

    void sendMailRejection(Onboarding onboarding);

    void persistOnboarding(Onboarding onboarding);

    void persistActivatedAt(Onboarding onboarding);

    void sendCompletedEmail(OnboardingWorkflow onboardingWorkflow);

    void sendCompletedEmailAggregate(Onboarding onboarding);

    void persistUsers(Onboarding onboarding);

    void createDelegation(Onboarding onboarding);

    Onboarding createAggregateOnboardingRequest(OnboardingAggregateOrchestratorInput onboardingAggregateOrchestratorInput);
}
