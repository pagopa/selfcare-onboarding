package it.pagopa.selfcare.onboarding.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.service.NotificationEventService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

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
        HttpResponseMessage responseMessage = function.sendNotifications(req, context);

        // Verify
        Mockito.verify(notificationEventService, times(1))
                .send(any(), any());
        assertEquals(HttpStatus.OK.value(), responseMessage.getStatusCode());

    }

    @Test
    public void sendNotificationTriggerError() {
        // Setup
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
        HttpResponseMessage responseMessage = function.sendNotifications(req, context);
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());

    }

}
