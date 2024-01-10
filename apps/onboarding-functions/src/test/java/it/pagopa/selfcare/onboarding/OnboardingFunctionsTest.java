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
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.functions.OnboardingFunctions;
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

import static it.pagopa.selfcare.onboarding.functions.CommonFunctions.SAVE_ONBOARDING_STATUS_ACTIVITY;
import static it.pagopa.selfcare.onboarding.functions.OnboardingFunctions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * Unit test for Function class.
 */
@QuarkusTest
public class OnboardingFunctionsTest {

    @Inject
    OnboardingFunctions function;

    @InjectMock
    OnboardingService service;

    final String onboardinString = "{\"onboardingId\":\"onboardingId\"}";

    /**
     * Unit test for HttpTriggerJava method.
     */
    @Test
    public void testHttpTriggerJava() throws Exception {
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
        doReturn(scheduleNewOrchestrationInstance).when(client).scheduleNewOrchestrationInstance("Onboardings",onboardingId);
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
        assertThrows(ResourceNotFoundException.class, () -> function.onboardingsOrchestrator(orchestrationContext));
    }

    @Test
    void onboardingsOrchestratorContractRegistration() {
        Onboarding onboarding = new Onboarding();
        onboarding.setOnboardingId("onboardingId");
        onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

        function.onboardingsOrchestrator(orchestrationContext);

        Mockito.verify(orchestrationContext, times(4))
                .callActivity(any(), any(), any(),any());

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(4))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(0));
        assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
        assertEquals(SEND_MAIL_REGISTRATION_WITH_CONTRACT_ACTIVITY, captorActivity.getAllValues().get(2));
        assertEquals(SAVE_ONBOARDING_STATUS_ACTIVITY, captorActivity.getAllValues().get(3));
    }

    @Test
    void onboardingsOrchestratorForApprove() {
        Onboarding onboarding = new Onboarding();
        onboarding.setOnboardingId("onboardingId");
        onboarding.setWorkflowType(WorkflowType.FOR_APPROVE);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

        function.onboardingsOrchestrator(orchestrationContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(2))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(SAVE_ONBOARDING_STATUS_ACTIVITY, captorActivity.getAllValues().get(1));
    }

    @Test
    void onboardingsOrchestratorConfirmation() {
        Onboarding onboarding = new Onboarding();
        onboarding.setOnboardingId("onboardingId");
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

        function.onboardingsOrchestrator(orchestrationContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(2))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(SEND_MAIL_CONFIRMATION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(SAVE_ONBOARDING_STATUS_ACTIVITY, captorActivity.getAllValues().get(1));
    }

    @Test
    void onboardingsOrchestratorRegistrationRequestApprove() {
        Onboarding onboarding = new Onboarding();
        onboarding.setOnboardingId("onboardingId");
        onboarding.setWorkflowType(WorkflowType.FOR_APPROVE_PT);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

        function.onboardingsOrchestrator(orchestrationContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(3))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY, captorActivity.getAllValues().get(1));
        assertEquals(SAVE_ONBOARDING_STATUS_ACTIVITY, captorActivity.getAllValues().get(2));
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

    @Test
    void buildContract() {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).createContract(any());

        function.buildContract(onboardinString, executionContext);

        Mockito.verify(service, times(1))
                .createContract(any());
    }

    @Test
    void saveToken() {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).saveTokenWithContract(any());

        function.saveToken(onboardinString, executionContext);

        Mockito.verify(service, times(1))
                .saveTokenWithContract(any());
    }

    @Test
    void sendMailRegistrationWithContract() {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailRegistrationWithContract(any());

        function.sendMailRegistrationWithContract(onboardinString, executionContext);

        Mockito.verify(service, times(1))
                .sendMailRegistrationWithContract(any());
    }

    @Test
    void sendMailRegistration() {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailRegistration(any());

        function.sendMailRegistration(onboardinString, executionContext);

        Mockito.verify(service, times(1))
                .sendMailRegistration(any());
    }

    @Test
    void sendMailRegistrationApprove() {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailRegistrationApprove(any());

        function.sendMailRegistrationApprove(onboardinString, executionContext);

        Mockito.verify(service, times(1))
                .sendMailRegistrationApprove(any());
    }

    @Test
    void sendMailOnboardingApprove() {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailOnboardingApprove(any());

        function.sendMailOnboardingApprove(onboardinString, executionContext);

        Mockito.verify(service, times(1))
                .sendMailOnboardingApprove(any());
    }
}
