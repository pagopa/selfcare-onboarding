package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.entity.Onboarding;

public interface CompletionService {

    String createInstitutionAndPersistInstitutionId(Onboarding onboarding);

    void sendMailRejection(Onboarding onboarding);

    void persistOnboarding(Onboarding onboarding);

    void persistActivatedAt(Onboarding onboarding);

    void sendCompletedEmail(Onboarding onboarding);

    void sendCompletedEmailAggregate(Onboarding onboarding);

    void persistUsers(Onboarding onboarding);
}
