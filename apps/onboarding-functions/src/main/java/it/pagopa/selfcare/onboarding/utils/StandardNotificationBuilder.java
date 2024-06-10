package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.BillingToSend;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;

import java.util.Objects;

public class StandardNotificationBuilder extends BaseNotificationBuilder {
    public StandardNotificationBuilder(
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
        notificationToSend.setInternalIstitutionID(institution.getId());
        notificationToSend.setNotificationType(queueEvent);
        return notificationToSend;
    }

    @Override
    public BillingToSend retrieveBilling(Onboarding onboarding) {
        if(Objects.isNull(onboarding.getBilling())) {
            return null;
        }

        BillingToSend billing = super.retrieveBilling(onboarding);
        billing.setPublicServices(onboarding.getBilling().isPublicServices());
        billing.setTaxCodeInvoicing(onboarding.getBilling().getTaxCodeInvoicing());
        return billing;
    }

    @Override
    public void setTokenData(NotificationToSend notificationToSend, Token token) {
        super.setTokenData(notificationToSend, token);
        notificationToSend.setFilePath(token.getContractSigned());
    }
}
