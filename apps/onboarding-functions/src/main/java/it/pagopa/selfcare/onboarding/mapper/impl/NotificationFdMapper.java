package it.pagopa.selfcare.onboarding.mapper.impl;

import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.NotificationType;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;

import java.util.Objects;

public class NotificationFdMapper extends NotificationCommonMapper {
    public NotificationFdMapper(String alternativeEmail,
                                InstitutionApi proxyRegistryInstitutionApi,
                                GeographicTaxonomiesApi geographicTaxonomiesApi,
                                org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi
    ) {
        super(alternativeEmail, proxyRegistryInstitutionApi, geographicTaxonomiesApi, coreInstitutionApi);
    }

    @Override
    public NotificationToSend toNotificationToSend(Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent) {
        NotificationToSend notificationToSend = super.toNotificationToSend(onboarding, token, institution, queueEvent);
        notificationToSend.setType(NotificationType.getNotificationTypeFromQueueEvent(queueEvent));
        clearFields(notificationToSend);
        return notificationToSend;
    }

    private void clearFields(NotificationToSend notificationToSend) {
        notificationToSend.setNotificationType(null);
        if(Objects.nonNull(notificationToSend.getBilling())) {
            notificationToSend.getBilling().setTaxCodeInvoicing(null);
        }
    }
}
