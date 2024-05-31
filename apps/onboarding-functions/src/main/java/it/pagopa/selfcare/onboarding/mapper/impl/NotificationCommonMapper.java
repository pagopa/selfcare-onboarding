package it.pagopa.selfcare.onboarding.mapper.impl;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.dto.InstitutionToNotify;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.dto.RootParent;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.mapper.NotificationMapper;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.core_json.model.PaymentServiceProviderResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.model.GeographicTaxonomyResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class NotificationCommonMapper implements NotificationMapper {
    private final String alternativeEmail;
    protected final InstitutionApi proxyRegistryInstitutionApi;
    protected final GeographicTaxonomiesApi geographicTaxonomiesApi;
    protected final org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi;
    protected static final String DESCRIPTION_TO_REPLACE_REGEX = " - COMUNE";
    protected static final Logger log = LoggerFactory.getLogger(NotificationCommonMapper.class);


    public NotificationCommonMapper(
            String alternativeEmail,
            InstitutionApi proxyRegistryInstitutionApi,
            GeographicTaxonomiesApi geographicTaxonomiesApi,
            org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi
    ) {
        this.alternativeEmail = alternativeEmail;
        this.proxyRegistryInstitutionApi = proxyRegistryInstitutionApi;
        this.geographicTaxonomiesApi = geographicTaxonomiesApi;
        this.coreInstitutionApi = coreInstitutionApi;
    }

    public NotificationToSend toNotificationToSend(Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent) {
        NotificationToSend notificationToSend = new NotificationToSend();
        if (queueEvent.equals(QueueEvent.ADD)) {
            // When Onboarding.complete event id is the onboarding id
            notificationToSend.setId(token.getId());
            notificationToSend.setState(convertOnboardingStatusToNotificationStatus(onboarding.getStatus()));
            // when onboarding complete last update is activated date
            notificationToSend.setUpdatedAt(OffsetDateTime.of(Optional.ofNullable(onboarding.getActivatedAt()).orElse(onboarding.getCreatedAt()), ZoneOffset.UTC));
        } else {
            // New id
            notificationToSend.setId(UUID.randomUUID().toString());
            notificationToSend.setState(convertOnboardingStatusToNotificationStatus(onboarding.getStatus()));
            if (onboarding.getStatus().equals(OnboardingStatus.DELETED)) {
                // Queue.ClosedAt: if token.deleted show closedAt
                notificationToSend.setClosedAt(OffsetDateTime.of(Optional.ofNullable(onboarding.getDeletedAt()).orElse(onboarding.getUpdatedAt()), ZoneOffset.UTC));
                notificationToSend.setUpdatedAt(OffsetDateTime.of(Optional.ofNullable(onboarding.getDeletedAt()).orElse(onboarding.getUpdatedAt()), ZoneOffset.UTC));
            } else {
                // when update last update is updated date
                notificationToSend.setUpdatedAt(OffsetDateTime.of(Optional.ofNullable(onboarding.getUpdatedAt()).orElse(onboarding.getCreatedAt()), ZoneOffset.UTC));
            }
        }
        notificationToSend.setInternalIstitutionID(institution.getId());
        notificationToSend.setNotificationType(queueEvent);

        mapDataFromToken(token, notificationToSend);
        mapDataFromOnboarding(onboarding, notificationToSend);
        mapDataFromInstitution(institution, notificationToSend);
        return notificationToSend;
    }

    private void mapDataFromOnboarding(Onboarding onboarding, NotificationToSend notificationToSend) {
        notificationToSend.setBilling(onboarding.getBilling());
        notificationToSend.setPricingPlan(onboarding.getPricingPlan());
        notificationToSend.setCreatedAt(OffsetDateTime.of(Optional.ofNullable(onboarding.getActivatedAt()).orElse(onboarding.getCreatedAt()), ZoneOffset.UTC));
    }

    private void mapDataFromToken(Token token, NotificationToSend notificationToSend) {
        notificationToSend.setProduct(token.getProductId());
        notificationToSend.setFilePath(token.getContractSigned());
        notificationToSend.setOnboardingTokenId(token.getId());
        notificationToSend.setFileName(Objects.isNull(token.getContractSigned()) ? "" : Paths.get(token.getContractSigned()).getFileName().toString());
        notificationToSend.setContentType(token.getContractSigned());
    }

    private String convertOnboardingStatusToNotificationStatus(OnboardingStatus status) {
        if (status.equals(OnboardingStatus.DELETED)) {
            return "CLOSED";
        } else  if (status.equals(OnboardingStatus.COMPLETED)) {
            return "ACTIVE";
        } else {
            return status.name();
        }
    }


    private void mapDataFromInstitution(InstitutionResponse institution, NotificationToSend notificationToSend) {
        InstitutionToNotify toNotify = new InstitutionToNotify();
        toNotify.setInstitutionType(InstitutionType.valueOf(institution.getInstitutionType().value()));
        toNotify.setDescription(institution.getDescription());
        toNotify.setDigitalAddress(institution.getDigitalAddress() == null ? alternativeEmail : institution.getDigitalAddress());
        toNotify.setAddress(institution.getAddress());
        toNotify.setTaxCode(institution.getTaxCode());
        toNotify.setOrigin(institution.getOrigin());
        toNotify.setOriginId(institution.getOriginId());
        toNotify.setZipCode(institution.getZipCode());
        toNotify.setPaymentServiceProvider(toSetPaymentServiceProvider(institution.getPaymentServiceProvider()));
        if (institution.getSubunitType() != null && !"EC".equals(institution.getSubunitType())) {
            toNotify.setSubUnitType(institution.getSubunitType());
            toNotify.setSubUnitCode(institution.getSubunitCode());
        }
        RootParent rootParent = new RootParent();
        if (Objects.nonNull(institution.getRootParent())) {
            rootParent.setId(institution.getRootParent().getId());
            rootParent.setDescription(institution.getRootParent().getDescription());
            InstitutionResponse parentInstitution = coreInstitutionApi.retrieveInstitutionByIdUsingGET(institution.getId());
            rootParent.setOriginId(Objects.nonNull(parentInstitution) ? parentInstitution.getOriginId() : null);
            toNotify.setRootParent(rootParent);
        }

        if (Objects.nonNull(institution.getAttributes()) && !institution.getAttributes().isEmpty()) {
            toNotify.setCategory(institution.getAttributes().get(0).getCode());
        }
        if (Objects.isNull(institution.getCity())) {
            setInstitutionLocation(toNotify, institution);
        } else {
            toNotify.setCounty(institution.getCounty());
            toNotify.setCountry(institution.getCountry());
            toNotify.setIstatCode(institution.getIstatCode());
            toNotify.setCity(institution.getCity().replace(DESCRIPTION_TO_REPLACE_REGEX, ""));
        }

        notificationToSend.setInstitution(toNotify);
    }

    private PaymentServiceProvider toSetPaymentServiceProvider(PaymentServiceProviderResponse paymentServiceProvider) {
        PaymentServiceProvider paymentServiceProviderToNotify = new PaymentServiceProvider();
        if (Objects.isNull(paymentServiceProvider)) {
            return null;
        }

        paymentServiceProviderToNotify.setAbiCode(paymentServiceProvider.getAbiCode());
        paymentServiceProviderToNotify.setBusinessRegisterNumber(paymentServiceProvider.getBusinessRegisterNumber());
        paymentServiceProviderToNotify.setLegalRegisterName(paymentServiceProvider.getLegalRegisterName());
        paymentServiceProviderToNotify.setLegalRegisterNumber(paymentServiceProvider.getLegalRegisterNumber());
        paymentServiceProviderToNotify.setVatNumberGroup(paymentServiceProvider.getVatNumberGroup());
        return paymentServiceProviderToNotify;
    }

    private void setInstitutionLocation(InstitutionToNotify toNotify, InstitutionResponse institution) {
        try {
            InstitutionResource institutionProxyInfo = proxyRegistryInstitutionApi.findInstitutionUsingGET(institution.getExternalId(), null, null);
            toNotify.setIstatCode(institutionProxyInfo.getIstatCode());
            toNotify.setCategory(institutionProxyInfo.getCategory());
            GeographicTaxonomyResource geographicTaxonomies = geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(toNotify.getIstatCode());
            toNotify.setCounty(geographicTaxonomies.getProvinceAbbreviation());
            toNotify.setCountry(geographicTaxonomies.getCountryAbbreviation());
            toNotify.setCity(geographicTaxonomies.getDesc().replace(DESCRIPTION_TO_REPLACE_REGEX, ""));
        } catch (Exception e) {
            log.warn("Error while searching institution {} on IPA, {} ", institution.getExternalId(), e.getMessage());
            toNotify.setIstatCode(null);
        }
    }

}
