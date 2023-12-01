package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.entity.Onboarding;

public interface CompletionService {

    void createInstitutionAndPersistInstitutionId(Onboarding onboarding);

    void sendCompletedEmail(Onboarding onboarding);
}
