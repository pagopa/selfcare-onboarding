package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.common.PricingPlan;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.*;
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
import java.util.Set;
import java.util.UUID;

public class SapNotificationBuilder extends BaseNotificationBuilder {
    private final UoApi proxyRegistryUoApi;
    private final AooApi proxyRegistryAooApi;

    public SapNotificationBuilder(
            String alternativeEmail,
            NotificationConfig.Consumer consumer,
            InstitutionApi proxyRegistryInstitutionApi,
            GeographicTaxonomiesApi geographicTaxonomiesApi,
            org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi,
            UoApi proxyRegistryUoApi,
            AooApi proxyRegistryAooApi
    ) {
        super(alternativeEmail, consumer, proxyRegistryInstitutionApi, geographicTaxonomiesApi, coreInstitutionApi);
        this.proxyRegistryUoApi = proxyRegistryUoApi;
        this.proxyRegistryAooApi = proxyRegistryAooApi;
    }
    @Override
    public NotificationToSend buildNotificationToSend(Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent) {
        NotificationToSend notificationToSend = super.buildNotificationToSend(onboarding, token, institution, queueEvent);
        notificationToSend.setId(UUID.randomUUID().toString());
        notificationToSend.setInstitutionId(institution.getId());
        notificationToSend.setType(NotificationType.getNotificationTypeFromQueueEvent(queueEvent));
        setNotificationToSendInstitutionDescription(notificationToSend);
        notificationToSend.getInstitution().setFileName(notificationToSend.getFileName());
        notificationToSend.getInstitution().setContentType(notificationToSend.getContentType());
        if (Objects.nonNull(onboarding.getBilling()) && Objects.nonNull(onboarding.getBilling().getTaxCodeInvoicing())) {
            // If there is tax code invoicing in billing, set it as taxCode in the institution
            notificationToSend.getInstitution().setTaxCode(onboarding.getBilling().getTaxCodeInvoicing());
        }
        return notificationToSend;
    }

    private static void setNotificationToSendInstitutionDescription(NotificationToSend notificationToSend) {
        if (notificationToSend.getInstitution().getRootParent() != null) {
            notificationToSend.getInstitution().setDescription(
                    notificationToSend.getInstitution().getRootParent().getDescription()
                            + " - " + notificationToSend.getInstitution().getDescription());
        }
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
    @Override
    public InstitutionToNotify retrieveInstitution(InstitutionResponse institution) {
        InstitutionToNotify institutionToNotify = super.retrieveInstitution(institution);

        // Field not allowed in SAP schema
        institutionToNotify.setCategory(null);
        return institutionToNotify;
    }
    @Override
    public void retrieveAndSetGeographicData(InstitutionToNotify institutionToNotify) {
        try {
            GeographicTaxonomyResource geographicTaxonomies;
            if (institutionToNotify.getSubUnitType() != null) {
                switch (Objects.requireNonNull(institutionToNotify.getSubUnitType())) {
                    case "UO" -> {
                        UOResource organizationUnit = proxyRegistryUoApi.findByUnicodeUsingGET1(institutionToNotify.getSubUnitCode(), null);
                        institutionToNotify.setIstatCode(organizationUnit.getCodiceComuneISTAT());
                        geographicTaxonomies = geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(organizationUnit.getCodiceComuneISTAT());
                    }
                    case "AOO" -> {
                        AOOResource homogeneousOrganizationalArea = proxyRegistryAooApi.findByUnicodeUsingGET(institutionToNotify.getSubUnitCode(), null);
                        institutionToNotify.setIstatCode(homogeneousOrganizationalArea.getCodiceComuneISTAT());
                        geographicTaxonomies = geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(homogeneousOrganizationalArea.getCodiceComuneISTAT());
                    }
                    default -> {
                        InstitutionResource proxyInfo = proxyRegistryInstitutionApi.findInstitutionUsingGET(institutionToNotify.getTaxCode(), null, null);
                        institutionToNotify.setIstatCode(proxyInfo.getIstatCode());
                        geographicTaxonomies = geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(proxyInfo.getIstatCode());
                    }
                }
            } else {
                InstitutionResource proxyInfo = proxyRegistryInstitutionApi.findInstitutionUsingGET(institutionToNotify.getTaxCode(), null, null);
                institutionToNotify.setIstatCode(proxyInfo.getIstatCode());
                geographicTaxonomies = geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(proxyInfo.getIstatCode());
            }

            if (geographicTaxonomies != null) {
                institutionToNotify.setCounty(geographicTaxonomies.getProvinceAbbreviation());
                institutionToNotify.setCountry(geographicTaxonomies.getCountryAbbreviation());
                institutionToNotify.setCity(geographicTaxonomies.getDesc().replace(DESCRIPTION_TO_REPLACE_REGEX, ""));
            }
        } catch (Exception e) {
            log.warn("Error while searching institution {} on IPA, {} ", institutionToNotify.getDescription(), e.getMessage());
            institutionToNotify.setIstatCode(null);
        }
    }
    @Override
    public boolean shouldSendNotification(Onboarding onboarding, InstitutionResponse institution) {
        return isProductAllowed(onboarding) && isAllowedInstitutionType(institution) && isAllowedOrigin(institution.getOrigin());
    }

    private boolean isProductAllowed(Onboarding onboarding) {
        // If the product is prodIo we can allow only Io Fast, and to do so we need to check pricing plan
        boolean isProdIo = ProductId.PROD_IO.name().equals(onboarding.getProductId());
        return !isProdIo || PricingPlan.FA.name().equals(onboarding.getPricingPlan());
    }

    private boolean isAllowedInstitutionType(InstitutionResponse institution) {
        return isNullOrEmpty(consumer.allowedInstitutionTypes()) || consumer.allowedInstitutionTypes().contains(institution.getInstitutionType().name());
    }

    private boolean isAllowedOrigin(String origin) {
        return isNullOrEmpty(consumer.allowedOrigins()) || consumer.allowedOrigins().contains(origin);
    }

    private boolean isNullOrEmpty(Set<String> set) {
        return Objects.isNull(set) || set.isEmpty();
    }
}
