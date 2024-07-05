package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.selfcare.onboarding.TestUtils.getMockedContext;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;


@QuarkusTest
class NotificationEventResenderServiceDefaultTest {
    @Inject
    NotificationEventResenderServiceDefault notificationEventResenderServiceDefault;

    @InjectMock
    NotificationEventService notificationEventService;

    @InjectMock
    OnboardingService onboardingService;

    @Test
    void resendNotifications() {
        // Arrange
        ResendNotificationsFilters filters = ResendNotificationsFilters.builder().onboardingId("test").build();
        ExecutionContext context = getMockedContext();

        Onboarding onboarding = new Onboarding();
        onboarding.setId("id1");
        Onboarding onboarding2 = new Onboarding();
        onboarding2.setId("id2");

        when(onboardingService.getOnboardingsToResend(filters, 0, 100)).thenReturn(List.of(onboarding, onboarding2));
        doNothing().when(notificationEventService).send(any(), any(), any());

        // Act
        ResendNotificationsFilters resendNotificationsFilters = notificationEventResenderServiceDefault.resendNotifications(filters, context);

        // Assert
        verify(notificationEventService, times(2)).send(any(), any(), any());
        verify(onboardingService).getOnboardingsToResend(filters, 0, 100);
        assertNull(resendNotificationsFilters);
    }

    @Test
    void resendNotificationsDoesntStopWhenSendProcessFails() {
        // Arrange
        ResendNotificationsFilters filters = new ResendNotificationsFilters();
        ExecutionContext context = getMockedContext();

        Onboarding onboarding = new Onboarding();
        onboarding.setId("id1");
        Onboarding onboarding2 = new Onboarding();
        onboarding2.setId("id2");

        doThrow(new NotificationException("Error")).when(notificationEventService).send(context, onboarding, null);
        when(onboardingService.getOnboardingsToResend(filters, 0, 100)).thenReturn(List.of(onboarding, onboarding2));

        // Act
        ResendNotificationsFilters resendNotificationsFilters = notificationEventResenderServiceDefault.resendNotifications(filters, context);

        // Assert
        verify(notificationEventService, times(2)).send(any(), any(), any());
        verify(onboardingService).getOnboardingsToResend(filters, 0, 100);
        assertNull(resendNotificationsFilters);
    }
}