package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.onboarding.service.profile.CheckOrganizationByPassTestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static it.pagopa.selfcare.onboarding.TestUtils.getMockedContext;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CheckOrganizationServiceDefaultTest {
    @Inject
    CheckOrganizationService checkOrganizationService;

    @Test
    void checkOrganizationWhenByPassCheckOrganizationIsFalse() {
        assertTrue(checkOrganizationService.checkOrganization(getMockedContext(), null, null));
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