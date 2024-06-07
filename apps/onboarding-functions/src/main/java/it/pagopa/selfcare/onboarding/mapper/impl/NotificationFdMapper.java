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
import java.util.UUID;

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
        notificationToSend.setId(UUID.randomUUID().toString());
        notificationToSend.setInstitutionId(notificationToSend.getInternalIstitutionID());
        notificationToSend.setType(NotificationType.getNotificationTypeFromQueueEvent(queueEvent));
        notificationToSend.getInstitution().setFileName(notificationToSend.getFileName());
        notificationToSend.getInstitution().setContentType(notificationToSend.getContentType());
        if(Objects.nonNull(notificationToSend.getBilling())) {
            notificationToSend.getBilling().setPublicService(notificationToSend.getBilling().isPublicServices());
        }
        clearFieldsNotNeeded(notificationToSend);
        return notificationToSend;
    }

    private void clearFieldsNotNeeded(NotificationToSend notificationToSend) {
        // This method is used to clear fields that are not needed in the notification object sent on FD topic
        notificationToSend.setInternalIstitutionID(null);
        notificationToSend.setNotificationType(null);
        notificationToSend.getInstitution().setCategory(null);
        if (Objects.nonNull(notificationToSend.getBilling())) {
            notificationToSend.getBilling().setPublicServices(null);
            notificationToSend.getBilling().setTaxCodeInvoicing(null);
        }
    }
}
