package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.*;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

public interface NotificationMapper {
    NotificationToSend toNotificationToSend(Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent);
}
