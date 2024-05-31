package it.pagopa.selfcare.onboarding.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.service.NotificationEventService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * Unit test for Notification Function class.
 */
@QuarkusTest
public class NotificationFunctionsTest {

    @Inject
    NotificationFunctions function;

    @InjectMock
    OnboardingService onboardingService;

    @InjectMock
    NotificationEventService notificationEventService;

    final String onboardinString = "{\"onboardingId\":\"onboardingId\"}";

    static ExecutionContext executionContext;

    static {
        executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    }

    @Test
    public void sendNotificationTrigger() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Optional<String> queryBody = Optional.of(onboardinString);
        doReturn(queryBody).when(req).getBody();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // Invoke
        HttpResponseMessage responseMessage = function.sendNotification(req, context);

        // Verify
        Mockito.verify(notificationEventService, times(1))
                .send(any(), any(), any());
        assertEquals(HttpStatus.OK.value(), responseMessage.getStatusCode());

    }

    @Test
    public void resendNotificationTrigger() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        final String onboardingId = "onboardingId";
        queryParams.put("onboardingId", onboardingId);
        doReturn(queryParams).when(req).getQueryParameters();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        when(onboardingService.getOnboarding(onboardingId)).thenReturn(Optional.of(new Onboarding()));

        // Invoke
        HttpResponseMessage responseMessage = function.resendNotification(req, context);

        // Verify
        Mockito.verify(notificationEventService, times(1))
                .send(any(), any(), any());
        assertEquals(HttpStatus.OK.value(), responseMessage.getStatusCode());

    }

    @Test
    public void resendNotificationNullOnboardingId() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        doReturn(queryParams).when(req).getQueryParameters();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // Invoke
        HttpResponseMessage responseMessage = function.resendNotification(req, context);
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());

    }

    @Test
    public void resendNotificationOnboardingNotFound() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        final String onboardingId = "onboardingId";
        queryParams.put("onboardingId", onboardingId);
        doReturn(queryParams).when(req).getQueryParameters();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        when(onboardingService.getOnboarding(onboardingId)).thenReturn(Optional.empty());

        // Invoke
        HttpResponseMessage responseMessage = function.resendNotification(req, context);
        assertEquals(HttpStatus.NOT_FOUND.value(), responseMessage.getStatusCode());

    }

    @Test
    public void sendNotificationTriggerError() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final String malformedOnboarding = "{\"onboardingId\":\"onboardingId\"";
        final Optional<String> queryBody = Optional.of(malformedOnboarding);
        doReturn(queryBody).when(req).getBody();
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));
        // Invoke
        HttpResponseMessage responseMessage = function.sendNotification(req, context);
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());

    }

}
