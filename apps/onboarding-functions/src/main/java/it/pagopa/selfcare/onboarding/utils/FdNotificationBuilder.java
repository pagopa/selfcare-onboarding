package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.*;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;

import java.util.Objects;
import java.util.UUID;

public class FdNotificationBuilder extends BaseNotificationBuilder {
    public FdNotificationBuilder(
            String alternativeEmail,
            NotificationConfig.Consumer consumer,
            InstitutionApi proxyRegistryInstitutionApi,
            GeographicTaxonomiesApi geographicTaxonomiesApi,
            org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi
    ) {
        super(alternativeEmail, consumer, proxyRegistryInstitutionApi, geographicTaxonomiesApi, coreInstitutionApi);
    }

    @Override
    public NotificationToSend buildNotificationToSend(Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent) {
        NotificationToSend notificationToSend = super.buildNotificationToSend(onboarding, token, institution, queueEvent);
        notificationToSend.setId(UUID.randomUUID().toString());
        notificationToSend.setInstitutionId(institution.getId());
        notificationToSend.setType(NotificationType.getNotificationTypeFromQueueEvent(queueEvent));
        notificationToSend.getInstitution().setFileName(notificationToSend.getFileName());
        notificationToSend.getInstitution().setContentType(notificationToSend.getContentType());
        return notificationToSend;
    }

    @Override
    public InstitutionToNotify retrieveInstitution(InstitutionResponse institution) {
        InstitutionToNotify institutionToNotify = super.retrieveInstitution(institution);

        // Field not allowed in FD schema
        institutionToNotify.setCategory(null);
        return institutionToNotify;
    }

    @Override
    public BillingToSend retrieveBilling(Onboarding onboarding) {
        if(Objects.isNull(onboarding.getBilling())) {
            return null;
        }

        BillingToSend billing = super.retrieveBilling(onboarding);
        billing.setPublicService(onboarding.getBilling().isPublicServices());
        return billing;
    }
}
