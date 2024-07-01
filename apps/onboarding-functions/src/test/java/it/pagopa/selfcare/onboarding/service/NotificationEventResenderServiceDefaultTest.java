package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;


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
        ResendNotificationsFilters filters = new ResendNotificationsFilters();
        ExecutionContext context = mockExecutionContext();

        Onboarding onboarding = new Onboarding();

        when(onboardingService.getOnboardingsToResend(filters, 0, 100)).thenReturn(List.of(onboarding));

        // Act
        notificationEventResenderServiceDefault.resendNotifications(filters, context);

        // Assert
        verify(notificationEventService).send(context, onboarding, null);
    }

    private ExecutionContext mockExecutionContext() {
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        return context;
    }
}