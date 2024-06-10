package it.pagopa.selfcare.onboarding.utils;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
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
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;

import java.time.OffsetDateTime;

import static it.pagopa.selfcare.onboarding.entity.Topic.SC_CONTRACTS_FD;
import static it.pagopa.selfcare.onboarding.utils.NotificationBuilderTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class FdNotificationBuilderTest {
    @InjectMock
    @RestClient
    InstitutionApi registryProxyInstitutionsApi;
    @InjectMock
    @RestClient
    GeographicTaxonomiesApi geographicTaxonomiesApi;
    @InjectMock
    @RestClient
    org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi;

    FdNotificationBuilder fdNotificationBuilder;

    @BeforeEach
    public void setup() {
        NotificationConfig.Consumer consumer = mock(NotificationConfig.Consumer.class);
        when(consumer.topic()).thenReturn(SC_CONTRACTS_FD.getValue());
        fdNotificationBuilder = new FdNotificationBuilder("alternativeEmail", consumer, registryProxyInstitutionsApi, geographicTaxonomiesApi, coreInstitutionApi);
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
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);

        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any()))
                .thenReturn(institutionParentResource);

        NotificationToSend notification = fdNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        assertNotNull(notification);
        assertNotEquals(onboarding.getId(), notification.getId());
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(tokenId, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getActivatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertNull(notification.getNotificationType());
        assertEquals(NotificationType.ADD_INSTITUTE, notification.getType());
        assertNull(notification.getBilling().getTaxCodeInvoicing());
        assertNull(notification.getBilling().isPublicServices());
        assertEquals(onboarding.getBilling().isPublicServices(), notification.getBilling().isPublicService());
        assertNull(notification.getFilePath());
    }
}