package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.onboarding.client.fd.FDRestClient;
import it.pagopa.selfcare.onboarding.dto.OrganizationLightBeanResponse;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.service.profile.CheckOrganizationByPassTestProfile;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static it.pagopa.selfcare.onboarding.TestUtils.getMockedContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class CheckOrganizationServiceDefaultTest {
    @Inject
    CheckOrganizationService checkOrganizationService;

    @InjectMock
    @RestClient
    FDRestClient fdRestClient;

    @Test
    void checkOrganizationSucceedsWhenFDApiInvocationSucceeds() {
        String fiscalCode = "fiscalCode";
        String vatNumber = "vatNumber";
        OrganizationLightBeanResponse organizationLightBeanResponse = new OrganizationLightBeanResponse();
        organizationLightBeanResponse.setAlreadyRegistered(true);
        when(fdRestClient.checkOrganization(fiscalCode, vatNumber)).thenReturn(organizationLightBeanResponse);
        assertTrue(checkOrganizationService.checkOrganization(getMockedContext(), fiscalCode, vatNumber));
    }

    @Test
    void checkOrganizationFailsWhenFDApiInvocationFails() {
        String fiscalCode = "fiscalCode";
        String vatNumber = "vatNumber";
        when(fdRestClient.checkOrganization(fiscalCode, vatNumber)).thenThrow(new RuntimeException());
        assertThrows(NotificationException.class, () -> checkOrganizationService.checkOrganization(getMockedContext(), fiscalCode, vatNumber));
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


}