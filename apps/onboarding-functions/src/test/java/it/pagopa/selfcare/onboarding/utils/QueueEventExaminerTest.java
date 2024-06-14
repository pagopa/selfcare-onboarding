package it.pagopa.selfcare.onboarding.utils;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class QueueEventExaminerTest {
    @Inject
    private QueueEventExaminer queueEventExaminer;

    @BeforeEach
    public void setup() {
        NotificationConfig notificationConfig = mock(NotificationConfig.class);
        when(notificationConfig.minutesThresholdForUpdateNotification()).thenReturn(5);
        queueEventExaminer = new QueueEventExaminer(notificationConfig);
    }

    @Test
    @DisplayName("Should return ADD event for COMPLETED status and update within threshold")
    public void shouldReturnAddEventForCompletedStatusAndUpdateWithinThreshold() {
        Onboarding onboarding = new Onboarding();
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.setUpdatedAt(LocalDateTime.now());
        onboarding.setActivatedAt(LocalDateTime.now().minusMinutes(4));

        QueueEvent result = queueEventExaminer.determineEventType(onboarding);

        assertEquals(QueueEvent.ADD, result);
    }

    @Test
    @DisplayName("Should return UPDATE event for COMPLETED status and update over threshold")
    public void shouldReturnUpdateEventForCompletedStatusAndUpdateOverThreshold() {
        Onboarding onboarding = new Onboarding();
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.setUpdatedAt(LocalDateTime.now());
        onboarding.setActivatedAt(LocalDateTime.now().minusMinutes(10));

        QueueEvent result = queueEventExaminer.determineEventType(onboarding);

        assertEquals(QueueEvent.UPDATE, result);
    }

    @Test
    @DisplayName("Should return UPDATE event for DELETED status")
    public void shouldReturnUpdateEventForDeletedStatus() {
        Onboarding onboarding = new Onboarding();
        onboarding.setStatus(OnboardingStatus.DELETED);

        QueueEvent result = queueEventExaminer.determineEventType(onboarding);

        assertEquals(QueueEvent.UPDATE, result);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for unsupported status")
    public void shouldThrowIllegalArgumentExceptionForUnsupportedStatus() {
        Onboarding onboarding = new Onboarding();
        onboarding.setStatus(OnboardingStatus.PENDING);

        assertThrows(IllegalArgumentException.class, () -> queueEventExaminer.determineEventType(onboarding));
    }
}