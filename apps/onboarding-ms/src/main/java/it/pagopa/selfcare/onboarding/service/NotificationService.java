package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;

public interface NotificationService {
    Uni<Void> resendOnboardingNotifications(OnboardingGetFilters filters);
}
