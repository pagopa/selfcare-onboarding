package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;

public interface NotificationEventResenderService {
    void resendNotifications(ResendNotificationsFilters filters, ExecutionContext context);
}
