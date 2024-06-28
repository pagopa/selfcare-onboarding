package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.onboarding.client.external.ExternalRestClient;
import it.pagopa.selfcare.onboarding.client.external.ExternalTokenRestClient;
import it.pagopa.selfcare.onboarding.dto.OauthToken;
import it.pagopa.selfcare.onboarding.dto.OrganizationLightBeanResponse;
import it.pagopa.selfcare.onboarding.dto.OrganizationResponse;
import it.pagopa.selfcare.onboarding.service.profile.CheckOrganizationByPassTestProfile;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.OffsetDateTime;

import static it.pagopa.selfcare.onboarding.TestUtils.getMockedContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class CheckOrganizationServiceDefaultTest {
    @Inject
    CheckOrganizationService checkOrganizationService;

    @InjectMock
    @RestClient
    ExternalRestClient externalRestClient;

    @InjectMock
    @RestClient
    ExternalTokenRestClient externalTokenRestClient;

    private static final String fiscalCode = "fiscalCode";
    private static final String vatNumber = "vatNumber";

    private static final String accessToken = "accessToken";

    @Test
    void checkOrganizationSucceedsWhenFDApiInvocationSucceeds() {
        when(externalTokenRestClient.getToken(any())).thenReturn(getDummyOauthToken());
        when(externalRestClient.checkOrganization(fiscalCode, vatNumber, "Bearer " + accessToken)).thenReturn(getDummyOrganizationLightBeanResponse());
        assertTrue(checkOrganizationService.checkOrganization(getMockedContext(), fiscalCode, vatNumber));
    }

    @Test
    void checkOrganizationFailsWhenFDApiInvocationFails() {
        when(externalTokenRestClient.getToken(any())).thenReturn(getDummyOauthToken());
        when(externalRestClient.checkOrganization(fiscalCode, vatNumber, "Bearer " + accessToken)).thenThrow(new RuntimeException());
        assertThrows(RuntimeException.class, () -> checkOrganizationService.checkOrganization(getMockedContext(), fiscalCode, vatNumber));
    }

    @Nested
    @TestProfile(CheckOrganizationByPassTestProfile.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class CheckOrganizationWhenByPassCheckOrganizationIsTrue {
        @Test
        void checkOrganizationWhenByPassCheckOrganizationIsTrue() {
            assertFalse(checkOrganizationService.checkOrganization(getMockedContext(), null, null));
        }
    }

    @Test
    void testTokenSucceedsWhenFDApiInvocationSucceeds() {
        when(externalTokenRestClient.getToken(any())).thenReturn(getDummyOauthToken());
        assertEquals(accessToken, checkOrganizationService.testToken(getMockedContext()));
    }

    private OrganizationLightBeanResponse getDummyOrganizationLightBeanResponse() {
        OrganizationLightBeanResponse organizationLightBeanResponse = new OrganizationLightBeanResponse();
        organizationLightBeanResponse.setAlreadyRegistered(true);
        OrganizationResponse organizationResponse = new OrganizationResponse();
        organizationResponse.setId("id");
        organizationResponse.setCodiceFiscale("fiscalCode");
        organizationResponse.setPartitaIva("vatNumber");
        organizationResponse.setLegalName("legalName");
        organizationResponse.setStatus("status");
        organizationResponse.setCity("city");
        organizationResponse.setProvince("province");
        organizationResponse.setAddress("address");
        organizationResponse.setStreetNumber("streetNumber");
        organizationResponse.setZipCode("zipCode");
        organizationResponse.setGarante(true);
        organizationResponse.setGarantito(true);
        organizationResponse.setContraente(true);
        organizationResponse.setTypeOfCounterparty("typeOfCounterparty");
        organizationResponse.setCreationDate(OffsetDateTime.now());
        organizationResponse.setActivationDate(OffsetDateTime.now());
        organizationLightBeanResponse.setOrganization(organizationResponse);
        return organizationLightBeanResponse;
    }

    private OauthToken getDummyOauthToken() {
        OauthToken oauthToken = new OauthToken();
        oauthToken.setAccessToken(accessToken);
        oauthToken.setExpiresIn("3600");
        return oauthToken;
    }
}