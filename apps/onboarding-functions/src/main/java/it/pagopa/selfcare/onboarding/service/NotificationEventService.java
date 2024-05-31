package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;

public interface NotificationEventService {
    void send(Onboarding onboarding, QueueEvent queueEvent);
}
