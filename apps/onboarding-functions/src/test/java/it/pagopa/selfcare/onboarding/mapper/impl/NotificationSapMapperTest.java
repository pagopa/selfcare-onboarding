package it.pagopa.selfcare.onboarding.mapper.impl;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.NotificationType;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.GeographicTaxonomyResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.time.OffsetDateTime;

import static it.pagopa.selfcare.onboarding.mapper.impl.NotificationMapperTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class NotificationSapMapperTest {
    @InjectMock
    @RestClient
    InstitutionApi registryProxyInstitutionsApi;
    @InjectMock
    @RestClient
    GeographicTaxonomiesApi geographicTaxonomiesApi;
    @InjectMock
    @RestClient
    org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi;
    @InjectMock
    @RestClient
    UoApi proxyRegistryUoApi;
    @InjectMock
    @RestClient
    AooApi proxyRegistryAooApi;
    
    static NotificationSapMapper notificationSapMapper;
    
    @BeforeEach
    public void setup() {
        notificationSapMapper = new NotificationSapMapper("alternativeEmail", registryProxyInstitutionsApi, geographicTaxonomiesApi, proxyRegistryUoApi, proxyRegistryAooApi, coreInstitutionApi);
    }

    @Test
    void toNotificationToSendForEc() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:02:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);

        // Create Institution
        InstitutionResponse institution = createInstitution();
        institution.setCity(null);
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any()))
                .thenReturn(institutionParentResource);

        when(registryProxyInstitutionsApi.findInstitutionUsingGET(institution.getExternalId(), null, null))
                .thenThrow(new RuntimeException("Error"));

        when(registryProxyInstitutionsApi.findInstitutionUsingGET("taxCodeInvoicing", null, null))
                .thenReturn(new InstitutionResource().istatCode("istatCode"));

        when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
                .thenReturn(new GeographicTaxonomyResource().country("country").provinceAbbreviation("provinceAbbreviation").countryAbbreviation("countryAbbreviation").desc("desc"));

        NotificationToSend notification = notificationSapMapper.toNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(tokenId, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getActivatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertNull(notification.getNotificationType());
        assertEquals(NotificationType.ADD_INSTITUTE, notification.getType());
        assertNull(notification.getBilling().getTaxCodeInvoicing());
        assertEquals("taxCodeInvoicing", notification.getInstitution().getTaxCode());
        assertEquals("istatCode", notification.getInstitution().getIstatCode());
        assertEquals("provinceAbbreviation", notification.getInstitution().getCounty());
        assertEquals("countryAbbreviation", notification.getInstitution().getCountry());
        assertEquals("desc", notification.getInstitution().getCity());
    }

    @Test
    void toNotificationToSendForAoo() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:02:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);

        // Create Institution
        InstitutionResponse institution = createInstitution();
        institution.setSubunitType("AOO");
        institution.setSubunitCode("subunitCode");
        institution.setCity(null);
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any()))
                .thenReturn(institutionParentResource);

        when(registryProxyInstitutionsApi.findInstitutionUsingGET(institution.getExternalId(), null, null)).thenThrow(new RuntimeException("Error"));

        when(proxyRegistryAooApi.findByUnicodeUsingGET(any(), any()))
                .thenReturn(new AOOResource().codiceComuneISTAT("istatCode"));
        when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
                .thenReturn(new GeographicTaxonomyResource().country("country").provinceAbbreviation("provinceAbbreviation").countryAbbreviation("countryAbbreviation").desc("desc"));

        NotificationToSend notification = notificationSapMapper.toNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(tokenId, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getActivatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertNull(notification.getNotificationType());
        assertEquals(NotificationType.ADD_INSTITUTE, notification.getType());
        assertNull(notification.getBilling().getTaxCodeInvoicing());
        assertEquals("taxCodeInvoicing", notification.getInstitution().getTaxCode());
        assertEquals("istatCode", notification.getInstitution().getIstatCode());
        assertEquals("provinceAbbreviation", notification.getInstitution().getCounty());
        assertEquals("countryAbbreviation", notification.getInstitution().getCountry());
        assertEquals("desc", notification.getInstitution().getCity());

        verify(proxyRegistryAooApi).findByUnicodeUsingGET("subunitCode", null);
        verify(geographicTaxonomiesApi).retrieveGeoTaxonomiesByCodeUsingGET("istatCode");
    }

    @Test
    void toNotificationToSendForUo() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:02:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);

        // Create Institution
        InstitutionResponse institution = createInstitution();
        institution.setSubunitType("UO");
        institution.setSubunitCode("subunitCode");
        institution.setCity(null);
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any()))
                .thenReturn(institutionParentResource);

        when(registryProxyInstitutionsApi.findInstitutionUsingGET(institution.getExternalId(), null, null)).thenThrow(new RuntimeException("Error"));

        when(proxyRegistryUoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(new UOResource().codiceComuneISTAT("istatCode"));
        when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
                .thenReturn(new GeographicTaxonomyResource().country("country").provinceAbbreviation("provinceAbbreviation").countryAbbreviation("countryAbbreviation").desc("desc"));

        NotificationToSend notification = notificationSapMapper.toNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(tokenId, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getActivatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertNull(notification.getNotificationType());
        assertEquals(NotificationType.ADD_INSTITUTE, notification.getType());
        assertNull(notification.getBilling().getTaxCodeInvoicing());
        assertEquals("taxCodeInvoicing", notification.getInstitution().getTaxCode());
        assertEquals("istatCode", notification.getInstitution().getIstatCode());
        assertEquals("provinceAbbreviation", notification.getInstitution().getCounty());
        assertEquals("countryAbbreviation", notification.getInstitution().getCountry());
        assertEquals("desc", notification.getInstitution().getCity());

        verify(proxyRegistryUoApi).findByUnicodeUsingGET1("subunitCode", null);
        verify(geographicTaxonomiesApi).retrieveGeoTaxonomiesByCodeUsingGET("istatCode");
    }

    @Test
    void toNotificationToSendForEcWhenPartyRegistryThrowsException() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:02:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);

        // Create Institution
        InstitutionResponse institution = createInstitution();
        institution.setCity(null);
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any()))
                .thenReturn(institutionParentResource);

        when(registryProxyInstitutionsApi.findInstitutionUsingGET(any(), any(), any()))
                .thenThrow(new RuntimeException("Error"));

        NotificationToSend notification = notificationSapMapper.toNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(tokenId, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getActivatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertNull(notification.getNotificationType());
        assertEquals(NotificationType.ADD_INSTITUTE, notification.getType());
        assertNull(notification.getBilling().getTaxCodeInvoicing());
        assertEquals("taxCodeInvoicing", notification.getInstitution().getTaxCode());
        assertNull(notification.getInstitution().getIstatCode());
    }
}