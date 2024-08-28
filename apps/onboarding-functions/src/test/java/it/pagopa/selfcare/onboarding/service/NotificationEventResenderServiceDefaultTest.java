package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static it.pagopa.selfcare.onboarding.TestUtils.getMockedContext;
import static org.junit.jupiter.api.Assertions.*;
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
    void resendNotificationsEndsWithNullIfIsWorkingOnLastPage() {
        // Arrange
        ResendNotificationsFilters filters = ResendNotificationsFilters.builder().onboardingId("test").build();
        ExecutionContext context = getMockedContext();

        when(onboardingService.getOnboardingsToResend(filters, 0, 100)).thenReturn(mockOnboardingList(2));
        doNothing().when(notificationEventService).send(any(), any(), any());

        // Act
        ResendNotificationsFilters resendNotificationsFilters = notificationEventResenderServiceDefault.resendNotifications(filters, context);

        // Assert
        verify(notificationEventService, times(2)).send(any(), any(), any(), any());
        verify(onboardingService).getOnboardingsToResend(filters, 0, 100);
        assertNull(resendNotificationsFilters);
    }

    @Test
    void resendNotificationsEndsIncrementingPageIfIsWorkingOnIntermediatePage() {
        // Arrange
        ResendNotificationsFilters filters = ResendNotificationsFilters.builder().onboardingId("test").build();
        ExecutionContext context = getMockedContext();

        when(onboardingService.getOnboardingsToResend(filters, 0, 100)).thenReturn(mockOnboardingList(100));
        doNothing().when(notificationEventService).send(any(), any(), any());

        // Act
        ResendNotificationsFilters resendNotificationsFilters = notificationEventResenderServiceDefault.resendNotifications(filters, context);

        // Assert
        verify(notificationEventService, times(100)).send(any(), any(), any(), any());
        verify(onboardingService).getOnboardingsToResend(filters, 0, 100);
        assertNotNull(resendNotificationsFilters);
        assertEquals(1, resendNotificationsFilters.getPage());
    }

    @Test
    void resendNotificationsActivityException() {
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        doThrow(new NotificationException("Error")).when(notificationEventService).send(any(), any(), any(), any());

        List<Onboarding> onboardings = new ArrayList<>();
        onboardings.add(new Onboarding());
        when(onboardingService.getOnboardingsToResend(any(), anyInt(), anyInt())).thenReturn(onboardings);

        ResendNotificationsFilters resendNotificationsFilters = new ResendNotificationsFilters();
        resendNotificationsFilters.setProductId("prod-pagopa");

        ResendNotificationsFilters nextFilter = notificationEventResenderServiceDefault.resendNotifications(resendNotificationsFilters, context);

        assertNull(nextFilter);
    }

    private List<Onboarding> mockOnboardingList(int size) {
        List<Onboarding> onboardings = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Onboarding onboarding = new Onboarding();
            onboarding.setId("id" + i);
            onboardings.add(onboarding);
        }
        return onboardings;
    }
}