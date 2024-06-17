package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import jakarta.inject.Inject;
import org.apache.hc.core5.http.HttpStatus;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.onboarding_functions_json.api.NotificationApi;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class NotificationServiceDefaultTest {

    @Inject
    NotificationServiceDefault notificationServiceDefault;

    @InjectMock
    @RestClient
    NotificationApi notificationApi;

    @Test
    @RunOnVertxContext
    @DisplayName("Should return void item and take in charge the notification resend")
    public void shouldReturnVoidItemAndTakeInChargeNotificationResend() {
        OnboardingGetFilters filters = OnboardingGetFilters.builder().status("COMPLETED").build();

        UniAssertSubscriber<Void> subscriber = notificationServiceDefault.resendOnboardingNotifications(filters)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(null);
    }

    @Test
    @RunOnVertxContext
    @DisplayName("Should try to send all notifications when notifications api calls succeed")
    public void shouldTryToSendAllNotifications() {
        OnboardingGetFilters filters = OnboardingGetFilters.builder().status("COMPLETED").build();
        mockOnboardingFind();
        when(notificationApi.apiNotificationPost(any(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        UniAssertSubscriber<Void> subscriber = notificationServiceDefault.asyncSendNotifications(filters)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().awaitSubscription().assertItem(null);
        verify(notificationApi, times(3)).apiNotificationPost(any(), any());
    }

    @Test
    @RunOnVertxContext
    @DisplayName("Should try to send all notifications when notifications api calls throw ignorable error")
    public void shouldTryToSendAllNotificationsWhenApiThrowIgnorableError() {
        OnboardingGetFilters filters = OnboardingGetFilters.builder().status("COMPLETED").build();
        mockOnboardingFind();

        when(notificationApi.apiNotificationPost(any(), any()))
                .thenReturn(Uni.createFrom().nullItem()) // first call
                .thenReturn(Uni.createFrom().failure(new ClientWebApplicationException(HttpStatus.SC_INTERNAL_SERVER_ERROR))) // second call
                .thenReturn(Uni.createFrom().nullItem()); // third call

        UniAssertSubscriber<Void> subscriber = notificationServiceDefault.asyncSendNotifications(filters)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().awaitSubscription();
        verify(notificationApi, times(3)).apiNotificationPost(any(), any());
    }

    @Test
    @RunOnVertxContext
    @DisplayName("Should not send all notifications when notifications api calls throw non ignorable error")
    public void shouldNotSendAllNotificationsWhenApiThrowNonIgnorableError() {
        OnboardingGetFilters filters = OnboardingGetFilters.builder().status("COMPLETED").build();
        mockOnboardingFind();

        when(notificationApi.apiNotificationPost(any(), any()))
                .thenReturn(Uni.createFrom().nullItem()) // first call
                .thenReturn(Uni.createFrom().failure(new ClientWebApplicationException(HttpStatus.SC_TOO_MANY_REQUESTS))); // second call

        UniAssertSubscriber<Void> subscriber = notificationServiceDefault.asyncSendNotifications(filters)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailed().awaitSubscription();
        verify(notificationApi, times(2)).apiNotificationPost(any(), any());
    }

    private void mockOnboardingFind() {
        ReactivePanacheQuery query = mock(ReactivePanacheQuery.class);
        ReactivePanacheQuery<Onboarding> queryPage = mock(ReactivePanacheQuery.class);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.find((Document) any(), any())).thenReturn(query);
        when(query.page(anyInt(), anyInt())).thenReturn(queryPage);
        List<Onboarding> onboardingList = List.of(createDummyOnboarding(), createDummyOnboarding(), createDummyOnboarding());
        when(queryPage.stream()).thenReturn(Multi.createFrom().iterable(onboardingList));

    }

    private Onboarding createDummyOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("id");
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        return onboarding;
    }

    @Test
    @DisplayName("Should resend notifications for deleted status")
    public void shouldResendNotificationsForDeletedStatus() {
        OnboardingGetFilters filters = OnboardingGetFilters.builder().status("DELETED").build();

        notificationServiceDefault.resendOnboardingNotifications(filters);
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when status is null")
    public void shouldThrowInvalidRequestExceptionWhenStatusIsNull() {
        OnboardingGetFilters filters = OnboardingGetFilters.builder().build();

        assertThrows(InvalidRequestException.class, () -> notificationServiceDefault.resendOnboardingNotifications(filters));
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when status is not COMPLETED or DELETED")
    public void shouldThrowInvalidRequestExceptionWhenStatusIsNotCompletedOrDeleted() {
        OnboardingGetFilters filters = OnboardingGetFilters.builder().status("INVALID_STATUS").build();

        assertThrows(InvalidRequestException.class, () -> notificationServiceDefault.resendOnboardingNotifications(filters));
    }
}