package it.pagopa.selfcare.onboarding.mapper.impl;

import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.NotificationType;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.GeographicTaxonomyResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.util.Objects;

public class NotificationSapMapper extends NotificationCommonMapper {
    private final UoApi proxyRegistryUoApi;
    private final AooApi proxyRegistryAooApi;
    public NotificationSapMapper(String alternativeEmail,
                                 InstitutionApi proxyRegistryInstitutionApi,
                                 GeographicTaxonomiesApi geographicTaxonomiesApi,
                                 UoApi proxyRegistryUoApi,
                                 AooApi proxyRegistryAooApi,
                                 org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi
    ) {
        super(alternativeEmail, proxyRegistryInstitutionApi, geographicTaxonomiesApi, coreInstitutionApi);
        this.proxyRegistryUoApi = proxyRegistryUoApi;
        this.proxyRegistryAooApi = proxyRegistryAooApi;
    }

    @Override
    public NotificationToSend toNotificationToSend(Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent) {
        NotificationToSend notificationToSend = super.toNotificationToSend(onboarding, token, institution, queueEvent);
        if (Objects.nonNull(onboarding.getBilling()) && Objects.nonNull(onboarding.getBilling().getTaxCodeInvoicing())) {
            notificationToSend.getInstitution().setTaxCode(onboarding.getBilling().getTaxCodeInvoicing());
        }
        notificationToSend.setType(NotificationType.getNotificationTypeFromQueueEvent(queueEvent));
        clearFields(notificationToSend);
        setNotificationInstitutionLocationFields(notificationToSend);
        setNotificationToSendInstitutionDescription(notificationToSend);
        return notificationToSend;
    }

    private void clearFields(NotificationToSend notificationToSend) {
        notificationToSend.setNotificationType(null);
        if (Objects.nonNull(notificationToSend.getBilling())) {
            notificationToSend.getBilling().setTaxCodeInvoicing(null);
        }
    }

    private static void setNotificationToSendInstitutionDescription(NotificationToSend notificationToSend) {
        if (notificationToSend.getInstitution().getRootParent() != null) {
            notificationToSend.getInstitution().setDescription(
                    notificationToSend.getInstitution().getRootParent().getDescription()
                            + " - " + notificationToSend.getInstitution().getDescription());
        }
    }

    private void setNotificationInstitutionLocationFields(NotificationToSend notificationToSend) {
        try {
            GeographicTaxonomyResource geographicTaxonomies = null;
            if (notificationToSend.getInstitution().getSubUnitType() != null && notificationToSend.getInstitution().getCity() == null) {
                switch (Objects.requireNonNull(notificationToSend.getInstitution().getSubUnitType())) {
                    case "UO" -> {
                        UOResource organizationUnit = proxyRegistryUoApi.findByUnicodeUsingGET1(notificationToSend.getInstitution().getSubUnitCode(), null);
                        notificationToSend.getInstitution().setIstatCode(organizationUnit.getCodiceComuneISTAT());
                        geographicTaxonomies = geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(organizationUnit.getCodiceComuneISTAT());
                    }
                    case "AOO" -> {
                        AOOResource homogeneousOrganizationalArea = proxyRegistryAooApi.findByUnicodeUsingGET(notificationToSend.getInstitution().getSubUnitCode(), null);
                        notificationToSend.getInstitution().setIstatCode(homogeneousOrganizationalArea.getCodiceComuneISTAT());
                        geographicTaxonomies = geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(homogeneousOrganizationalArea.getCodiceComuneISTAT());
                    }
                    default -> {
                        InstitutionResource proxyInfo = proxyRegistryInstitutionApi.findInstitutionUsingGET(notificationToSend.getInstitution().getTaxCode(), null, null);
                        geographicTaxonomies = geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(proxyInfo.getIstatCode());
                        notificationToSend.getInstitution().setIstatCode(proxyInfo.getIstatCode());
                    }
                }
            }
            if (geographicTaxonomies != null) {
                notificationToSend.getInstitution().setCounty(geographicTaxonomies.getProvinceAbbreviation());
                notificationToSend.getInstitution().setCountry(geographicTaxonomies.getCountryAbbreviation());
                notificationToSend.getInstitution().setCity(geographicTaxonomies.getDesc().replace(DESCRIPTION_TO_REPLACE_REGEX, ""));
            }
        } catch (Exception e) {
            log.warn("Error while searching institution {} on IPA, {} ", notificationToSend.getInstitution().getDescription(), e.getMessage());
            notificationToSend.getInstitution().setIstatCode(null);
        }
    }
}
