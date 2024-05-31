package it.pagopa.selfcare.onboarding.mapper.impl;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.core_json.model.AttributesResponse;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;

import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.selfcare.onboarding.mapper.impl.NotificationMapperTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class NotificationCommonMapperTest {
    @InjectMock
    @RestClient
    InstitutionApi registryProxyInstitutionsApi;
    @InjectMock
    @RestClient
    GeographicTaxonomiesApi geographicTaxonomiesApi;
    @InjectMock
    @RestClient
    org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi;

    NotificationCommonMapper notificationCommonMapper;

    @BeforeEach
    public void setup() {
        notificationCommonMapper = new NotificationCommonMapper("alternativeEmail", registryProxyInstitutionsApi, geographicTaxonomiesApi, coreInstitutionApi);
    }

    @Test
    void toNotificationToSendWhenOnboardingHasActivatedAtAndQueueEventAdd() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:02:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any()))
                .thenReturn(institutionParentResource);

        NotificationToSend notification = notificationCommonMapper.toNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(tokenId, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getActivatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertEquals(QueueEvent.ADD, notification.getNotificationType());
    }

    @Test
    void toNotificationToSendWhenOnboardingHasNotActivatedAtAndQueueEventAdd() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                null, // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any()))
                .thenReturn(institutionParentResource);

        NotificationToSend notification = notificationCommonMapper.toNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(tokenId, notification.getOnboardingTokenId());
        assertEquals(onboarding.getCreatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getCreatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertEquals(QueueEvent.ADD, notification.getNotificationType());
    }

    @Test
    void toNotificationToSendWhenOnboardingHasActivatedAtAndQueueEventUpdate() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:02:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any()))
                .thenReturn(institutionParentResource);

        NotificationToSend notification = notificationCommonMapper.toNotificationToSend(onboarding, token, institution, QueueEvent.UPDATE);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(tokenId, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getUpdatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertEquals(QueueEvent.UPDATE, notification.getNotificationType());
    }

    @Test
    void toNotificationToSendWhenOnboardingHasNotActivatedAtAndQueueEventUpdate() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:07:00Z"), // updatedAt
                null // deletedAt
        );
        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any()))
                .thenReturn(institutionParentResource);

        NotificationToSend notification = notificationCommonMapper.toNotificationToSend(onboarding, token, institution, QueueEvent.UPDATE);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(tokenId, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getUpdatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertEquals(QueueEvent.UPDATE, notification.getNotificationType());
    }

    @Test
    void toNotificationToSendWhenOnboardingDeletedHasDeletedAtAndQueueEventUpdate() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.DELETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:06:00Z"), // updatedAt
                OffsetDateTime.parse("2020-12-02T18:22:00Z") // deletedAt
        );
        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any()))
                .thenReturn(institutionParentResource);

        NotificationToSend notification = notificationCommonMapper.toNotificationToSend(onboarding, token, institution, QueueEvent.UPDATE);

        assertNotNull(notification);
        assertEquals("CLOSED", notification.getState());
        assertEquals(tokenId, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getDeletedAt(), notification.getClosedAt().toLocalDateTime());
        assertEquals(onboarding.getDeletedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertEquals(QueueEvent.UPDATE, notification.getNotificationType());
    }

    @Test
    void toNotificationToSendWhenOnboardingDeletedHasNotDeletedAtAndQueueEventUpdate() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.DELETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:06:00Z"), // updatedAt
                null // deletedAt
        );
        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any()))
                .thenReturn(institutionParentResource);

        NotificationToSend notification = notificationCommonMapper.toNotificationToSend(onboarding, token, institution, QueueEvent.UPDATE);

        assertNotNull(notification);
        assertEquals("CLOSED", notification.getState());
        assertEquals(tokenId, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getUpdatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertEquals(onboarding.getUpdatedAt(), notification.getClosedAt().toLocalDateTime());
        assertEquals(QueueEvent.UPDATE, notification.getNotificationType());
    }

    @Test
    void toNotificationAttributesNotNull() {

        // Create Institution
        InstitutionResponse institution = createInstitution();
        AttributesResponse attribute = new AttributesResponse();
        attribute.setCode("code");
        institution.setAttributes(List.of(attribute));
        // Create Token
        Token token = createToken();
        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                null, // activatedAt
                OffsetDateTime.parse("2020-11-02T10:00:00Z"), // updatedAt
                null
        );

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any()))
                .thenReturn(institutionParentResource);


        //when
        NotificationToSend notificationToSend = notificationCommonMapper.toNotificationToSend(onboarding, token, institution, QueueEvent.ADD);
        //then
        assertNotNull(notificationToSend);
        verifyNoInteractions(registryProxyInstitutionsApi);
        verifyNoInteractions(geographicTaxonomiesApi);
    }

    @Test
    void toNotificationCityNull() {

        // Create Institution
        InstitutionResponse institution = createInstitution();
        institution.setCity(null);
        // Create Token
        Token token = createToken();
        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                null, // activatedAt
                OffsetDateTime.parse("2020-11-02T10:00:00Z"), // updatedAt
                null
        );

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any()))
                .thenReturn(institutionParentResource);

        mockPartyRegistryProxy(registryProxyInstitutionsApi, geographicTaxonomiesApi, institution);

        //when
        NotificationToSend notificationToSend = notificationCommonMapper.toNotificationToSend(onboarding, token, institution, QueueEvent.ADD);
        //then
        assertNotNull(notificationToSend);
        verify(registryProxyInstitutionsApi).findInstitutionUsingGET(any(), any(), any());
        verify(geographicTaxonomiesApi).retrieveGeoTaxonomiesByCodeUsingGET(any());

    }


}