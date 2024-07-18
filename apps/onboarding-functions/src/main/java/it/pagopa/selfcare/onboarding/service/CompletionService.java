package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
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

    String createDelegation(Onboarding onboarding);

    String createAggregateOnboardingRequest(OnboardingAggregateOrchestratorInput onboardingAggregateOrchestratorInput);

    void sendTestEmail(ExecutionContext context);
}
