package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.onboarding_functions_json.api.NotificationsApi;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

@QuarkusTest
class NotificationServiceDefaultTest {

    @Inject
    NotificationServiceDefault notificationServiceDefault;

    @InjectMock
    OnboardingMapper onboardingMapper;

    @InjectMock
    @RestClient
    NotificationsApi notificationsApi;

    @Test
    @RunOnVertxContext
    @DisplayName("Should resend notifications for completed status")
    public void shouldResendNotificationsForCompletedStatus(UniAsserter asserter) {
        OnboardingGetFilters filters = OnboardingGetFilters.builder().status("COMPLETED").build();
        mockOnboardingFind(asserter);
        mockNotificationsApi();

        UniAssertSubscriber<Void> subscriber = notificationServiceDefault.resendOnboardingNotifications(filters)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().awaitSubscription();
    }

    private void mockOnboardingFind(UniAsserter asserter) {
        ReactivePanacheQuery query = mock(ReactivePanacheQuery.class);
        ReactivePanacheQuery<Onboarding> queryPage = mock(ReactivePanacheQuery.class);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.find((Document) any(), any())).thenReturn(query);
        when(query.page(anyInt(), anyInt())).thenReturn(queryPage);
        when(queryPage.stream()).thenReturn(Multi.createFrom().iterable(List.of(createDummyOnboarding())));

    }

    private Onboarding createDummyOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("id");
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        return onboarding;
    }

    private void mockNotificationsApi() {
        when(notificationsApi.apiNotificationsPost(any(), any())).thenReturn(Uni.createFrom().nullItem());
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