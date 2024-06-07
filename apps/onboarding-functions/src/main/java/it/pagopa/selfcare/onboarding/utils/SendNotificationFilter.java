package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

public interface SendNotificationFilter {
    boolean shouldSendNotification(Onboarding onboarding, InstitutionResponse institution);
}
