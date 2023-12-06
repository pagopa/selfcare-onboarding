package it.pagopa.selfcare.onboarding;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.CompletionService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static it.pagopa.selfcare.onboarding.OnboardingCompletionFunctions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
public class OnboardingCompletionFunctionsTest {


    @Inject
    OnboardingCompletionFunctions function;

    @InjectMock
    OnboardingService service;

    @InjectMock
    CompletionService completionService;

    final String onboardinString = "{\"onboardingId\":\"onboardingId\"}";

    /**
     * Unit test for HttpTriggerJava method.
     */
    @Test
    public void testHttpTriggerJava() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final HttpResponseMessage res = mock(HttpResponseMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        final String onboardingId = "onboardingId";
        queryParams.put("onboardingId", onboardingId);
        doReturn(queryParams).when(req).getQueryParameters();

        final Optional<String> queryBody = Optional.empty();
        doReturn(queryBody).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        final DurableClientContext durableContext = mock(DurableClientContext.class);
        final DurableTaskClient client = mock(DurableTaskClient.class);
        final String scheduleNewOrchestrationInstance = "scheduleNewOrchestrationInstance";
        doReturn(client).when(durableContext).getClient();
        doReturn(scheduleNewOrchestrationInstance).when(client).scheduleNewOrchestrationInstance(ONBOARDING_COMPLETION_ACTIVITY, onboardingId);
        doReturn(res).when(durableContext).createCheckStatusResponse(any(), any());

        // Invoke
        function.startOrchestration(req, durableContext, context);

        // Verify
        ArgumentCaptor<String> captorInstanceId = ArgumentCaptor.forClass(String.class);
        Mockito.verify(durableContext, times(1))
                .createCheckStatusResponse(any(), captorInstanceId.capture());
        assertEquals(scheduleNewOrchestrationInstance, captorInstanceId.getValue());
    }



    @Test
    void onboardingsOrchestrator_thorwExceptionIfOnboardingNotPresent() {
        final String onboardingId = "onboardingId";
        TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
        when(orchestrationContext.getInput(String.class)).thenReturn(onboardingId);
        when(service.getOnboarding(onboardingId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> function.onboardingCompletionOrchestrator(orchestrationContext));
    }


    @Test
    void onboardingCompletionOrchestrator() {
        Onboarding onboarding = new Onboarding();
        onboarding.setOnboardingId("onboardingId");

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

        function.onboardingCompletionOrchestrator(orchestrationContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(3))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
        assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(2));
    }


    @Test
    void createInstitutionAndPersistInstitutionId() {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(completionService).createInstitutionAndPersistInstitutionId(any());

        function.createInstitutionAndPersistInstitutionId(onboardinString, executionContext);

        Mockito.verify(completionService, times(1))
                .createInstitutionAndPersistInstitutionId(any());
    }

    @Test
    void sendCompletedEmail() {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(completionService).sendCompletedEmail(any());

        function.sendMailCompletion(onboardinString, executionContext);

        Mockito.verify(completionService, times(1))
                .sendCompletedEmail(any());
    }

    TaskOrchestrationContext mockTaskOrchestrationContext(Onboarding onboarding) {
        TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
        when(orchestrationContext.getInput(String.class)).thenReturn(onboarding.getOnboardingId());
        when(service.getOnboarding(onboarding.getOnboardingId())).thenReturn(Optional.of(onboarding));

        Task task = mock(Task.class);
        when(orchestrationContext.callActivity(any(),any(),any(),any())).thenReturn(task);
        when(task.await()).thenReturn("example");
        return orchestrationContext;
    }
}
