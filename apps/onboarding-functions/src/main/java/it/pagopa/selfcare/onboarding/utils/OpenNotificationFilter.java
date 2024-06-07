package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.enterprise.context.ApplicationScoped;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

@ApplicationScoped
public class OpenNotificationFilter implements SendNotificationFilter {
    @Override
    public boolean shouldSendNotification(Onboarding onboarding, InstitutionResponse institution) {
        return true;
    }
}
