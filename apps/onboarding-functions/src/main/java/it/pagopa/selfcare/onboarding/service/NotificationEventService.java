package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.entity.Onboarding;

public interface NotificationEventService {

    void send(Onboarding onboarding);
}
