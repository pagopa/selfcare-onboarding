package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;

public interface CompletionService {

    String createInstitutionAndPersistInstitutionId(Onboarding onboarding);

    void sendMailRejection(Onboarding onboarding);

    void persistOnboarding(Onboarding onboarding);

    void persistActivatedAt(Onboarding onboarding);

    void sendCompletedEmail(OnboardingWorkflow onboardingWorkflow);

    void persistUsers(Onboarding onboarding);
}
