package it.pagopa.selfcare.onboarding.functions;

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
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Institution;
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

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
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

    @InjectMock
    CompletionService completionService;

    final String onboardinString = "{\"onboardingId\":\"onboardingId\"}";

    static ExecutionContext executionContext;

    static {
        executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    }
    @Test
    public void startAndWaitOrchestration_failedOrchestration() throws Exception {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        final String onboardingId = "onboardingId";
        queryParams.put("onboardingId", onboardingId);
        queryParams.put("timeout", "10");
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

        // Invoke
        HttpResponseMessage responseMessage = function.startOrchestration(req, durableContext, context);

        // Verify
        Mockito.verify(client, times(1))
                .waitForInstanceCompletion(anyString(), any(), anyBoolean());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseMessage.getStatusCode());

    }

    @Test
    void onboardingsOrchestrator_throwExceptionIfOnboardingNotPresent() {
        final String onboardingId = "onboardingId";
        TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
        
        when(orchestrationContext.getInput(String.class)).thenReturn(onboardingId);
        when(service.getOnboarding(onboardingId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> function.onboardingsOrchestrator(orchestrationContext, executionContext));

        Mockito.verify(service, times(1))
                .updateOnboardingStatusAndInstanceId(onboardingId, OnboardingStatus.FAILED, orchestrationContext.getInstanceId());
    }

    @Test
    void onboardingsOrchestratorContractRegistration() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.REQUEST);
        onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(3))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(0));
        assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
        assertEquals(SEND_MAIL_REGISTRATION_FOR_CONTRACT, captorActivity.getAllValues().get(2));

        Mockito.verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.PENDING);
    }

    @Test
    void onboardingsOrchestratorForApprove() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.REQUEST);
        onboarding.setWorkflowType(WorkflowType.FOR_APPROVE);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
        

        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(1))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY, captorActivity.getAllValues().get(0));

        Mockito.verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.TOBEVALIDATED);
    }

    @Test
    void onboardingsOrchestratorForApproveWhenToBeValidated() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);
        onboarding.setWorkflowType(WorkflowType.FOR_APPROVE);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
        

        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(3))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(0));
        assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
        assertEquals(SEND_MAIL_REGISTRATION_FOR_CONTRACT_WHEN_APPROVE_ACTIVITY, captorActivity.getAllValues().get(2));

        Mockito.verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.PENDING);
    }

    @Test
    void onboardingsOrchestratorConfirmation() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.PENDING);
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION);
        onboarding.setInstitution(new Institution());

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
        

        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(3))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
        assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(2));

        Mockito.verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
    }

    @Test
    void onboardingsOrchestratorImport() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.PENDING);
        onboarding.setWorkflowType(WorkflowType.IMPORT);
        onboarding.setInstitution(new Institution());

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(2))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));

        Mockito.verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
    }

    @Test
    void onboardingsOrchestratorRegistrationRequestApprove() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.REQUEST);
        onboarding.setWorkflowType(WorkflowType.FOR_APPROVE_PT);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
        

        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(2))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY, captorActivity.getAllValues().get(1));

        Mockito.verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.TOBEVALIDATED);
    }



    @Test
    void onboardingsOrchestratorForApprovePtWhenToBeValidated() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setInstitution(new Institution());
        onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);
        onboarding.setWorkflowType(WorkflowType.FOR_APPROVE_PT);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(3))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
        assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(2));

        Mockito.verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
    }

    TaskOrchestrationContext mockTaskOrchestrationContext(Onboarding onboarding) {
        TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
        when(orchestrationContext.getInput(String.class)).thenReturn(onboarding.getId());
        when(service.getOnboarding(onboarding.getId())).thenReturn(Optional.of(onboarding));

        Task task = mock(Task.class);
        when(orchestrationContext.callActivity(any(),any(),any(),any())).thenReturn(task);
        when(task.await()).thenReturn("example");
        return orchestrationContext;
    }

    @Test
    void buildContract() {

        doNothing().when(service).createContract(any());

        function.buildContract(onboardinString, executionContext);

        Mockito.verify(service, times(1))
                .createContract(any());
    }

    @Test
    void saveToken() {
        
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).saveTokenWithContract(any());

        function.saveToken(onboardinString, executionContext);

        Mockito.verify(service, times(1))
                .saveTokenWithContract(any());
    }

    @Test
    void sendMailRegistrationWithContract() {
        
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailRegistrationForContract(any());

        function.sendMailRegistrationForContract(onboardinString, executionContext);

        Mockito.verify(service, times(1))
                .sendMailRegistrationForContract(any());
    }

    @Test
    void sendMailRegistration() {
        
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailRegistration(any());

        function.sendMailRegistration(onboardinString, executionContext);

        Mockito.verify(service, times(1))
                .sendMailRegistration(any());
    }

    @Test
    void sendMailRegistrationApprove() {
        
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailRegistrationApprove(any());

        function.sendMailRegistrationApprove(onboardinString, executionContext);

        Mockito.verify(service, times(1))
                .sendMailRegistrationApprove(any());
    }

    @Test
    void sendMailOnboardingApprove() {
        
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailOnboardingApprove(any());

        function.sendMailOnboardingApprove(onboardinString, executionContext);

        Mockito.verify(service, times(1))
                .sendMailOnboardingApprove(any());
    }

    @Test
    void sendMailRegistrationWithContractWhenApprove() {
        
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailRegistrationForContractWhenApprove(any());

        function.sendMailRegistrationForContractWhenApprove(onboardinString, executionContext);

        Mockito.verify(service, times(1))
                .sendMailRegistrationForContractWhenApprove(any());
    }


    @Test
    void onboardingCompletionOrchestrator() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.PENDING);
        onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);
        onboarding.setInstitution(new Institution());

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
        

        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(3))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
        assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(2));

        Mockito.verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
    }




    @Test
    void onboardingRejectedOrchestrator() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.REJECTED);
        onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);
        onboarding.setInstitution(new Institution());

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
        

        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(1))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(SEND_MAIL_REJECTION_ACTIVITY, captorActivity.getAllValues().get(0));
    }

    @Test
    void createInstitutionAndPersistInstitutionId() {

        final String institutionId = "institutionId";
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        when(completionService.createInstitutionAndPersistInstitutionId(any()))
                .thenReturn(institutionId);

        String actualInstitutionId = function.createInstitutionAndPersistInstitutionId(onboardinString, executionContext);

        assertEquals(institutionId, actualInstitutionId);
        Mockito.verify(completionService, times(1))
                .createInstitutionAndPersistInstitutionId(any());
    }

    @Test
    void sendCompletedEmail() {
        
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(completionService).sendCompletedEmail(any());

        function.sendMailCompletion(onboardinString, executionContext);

        Mockito.verify(completionService, times(1))
                .sendCompletedEmail(any());
    }

    @Test
    void sendMailRejection() {
        
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(completionService).sendMailRejection(any());

        function.sendMailRejection(onboardinString, executionContext);

        Mockito.verify(completionService, times(1))
                .sendMailRejection(any());
    }
}
