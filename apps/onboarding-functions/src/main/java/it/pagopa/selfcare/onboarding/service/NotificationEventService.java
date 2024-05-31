package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.entity.Onboarding;

public interface NotificationEventService {

    void send(ExecutionContext context, Onboarding onboarding);
}
