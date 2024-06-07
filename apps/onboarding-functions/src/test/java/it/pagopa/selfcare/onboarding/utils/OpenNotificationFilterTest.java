package it.pagopa.selfcare.onboarding.utils;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class OpenNotificationFilterTest {
    @Inject
    private OpenNotificationFilter openNotificationFilter;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should always allow notification")
    public void shouldAllowNotificationForAllowedInstitutionTypeAndOrigin() {
        assertTrue(openNotificationFilter.shouldSendNotification(new Onboarding(), new InstitutionResponse()));
    }
}