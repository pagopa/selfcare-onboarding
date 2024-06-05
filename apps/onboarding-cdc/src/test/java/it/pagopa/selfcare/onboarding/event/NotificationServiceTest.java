package it.pagopa.selfcare.onboarding.event;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.event.entity.Onboarding;
import it.pagopa.selfcare.onboarding.event.entity.util.QueueEvent;
import it.pagopa.selfcare.onboarding.event.mapper.OnboardingMapper;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.openapi.quarkus.onboarding_functions_json.api.NotificationsApi;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@QuarkusTest
public class NotificationServiceTest {
    @Mock
    private OnboardingMapper onboardingMapper;
    @InjectMock
    @RestClient
    private NotificationsApi notificationsApi;
    @Inject
    private NotificationService notificationService;

    @Test
    @DisplayName("Should handle Invoke Notification API Success passing event ADD")
    public void shouldHandleInvokeNotificationApiSuccessForQueueEventAdd() {
        Onboarding onboarding = new Onboarding();
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.setUpdatedAt(LocalDateTime.now());
        onboarding.setActivatedAt(LocalDateTime.now());

        when(notificationsApi.apiNotificationPost(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse()));

        UniAssertSubscriber<OrchestrationResponse> subscriber = notificationService
                .invokeNotificationApi(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().awaitItem();

        verify(notificationsApi, times(1)).apiNotificationPost(eq(QueueEvent.ADD.name()), any());
    }

    @Test
    @DisplayName("Should handle Invoke Notification API Success passing event UPDATE")
    public void shouldHandleInvokeNotificationApiSuccessForQueueEventUpdate() {
        Onboarding onboarding = new Onboarding();
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.setUpdatedAt(LocalDateTime.now().plusMinutes(10)); // 5 minutes should be the threshold
        onboarding.setActivatedAt(LocalDateTime.now());

        when(notificationsApi.apiNotificationPost(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse()));

        UniAssertSubscriber<OrchestrationResponse> subscriber = notificationService
                .invokeNotificationApi(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().awaitItem();

        verify(notificationsApi, times(1)).apiNotificationPost(eq(QueueEvent.UPDATE.name()), any());
    }

    @Test
    @DisplayName("Should handle Invoke Notification API Success passing event UPDATE with status DELETED")
    public void shouldHandleInvokeNotificationApiSuccessForQueueEventUpdateWithStatusDeleted() {
        Onboarding onboarding = new Onboarding();
        onboarding.setStatus(OnboardingStatus.DELETED);
        onboarding.setUpdatedAt(LocalDateTime.now()); // 5 minutes should be the threshold
        onboarding.setActivatedAt(LocalDateTime.now());

        when(notificationsApi.apiNotificationPost(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse()));

        UniAssertSubscriber<OrchestrationResponse> subscriber = notificationService
                .invokeNotificationApi(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().awaitItem();

        verify(notificationsApi, times(1)).apiNotificationPost(eq(QueueEvent.UPDATE.name()), any());
    }
}