package it.pagopa.selfcare.onboarding.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskFailedException;
import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.AggregateInstitution;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.CompletionService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.List;
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
class OnboardingFunctionsTest {

    @Inject
    OnboardingFunctions function;

    @InjectMock
    OnboardingService service;

    @InjectMock
    CompletionService completionService;

    final String onboardinString = "{\"onboardingId\":\"onboardingId\"}";

    final String onboardingWorkflowString = "{\"type\":\"INSTITUTION\",\"onboarding\":{\"id\":\"id\",\"productId\":null,\"testEnvProductIds\":null,\"workflowType\":null,\"institution\":null,\"users\":null,\"aggregates\":null,\"pricingPlan\":null,\"billing\":null,\"signContract\":null,\"expiringDate\":null,\"status\":null,\"userRequestUid\":null,\"workflowInstanceId\":null,\"createdAt\":null,\"updatedAt\":null,\"activatedAt\":null,\"deletedAt\":null,\"reasonForReject\":null,\"isAggregator\":null}}";

    static ExecutionContext executionContext;

    static {
        executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    }

    @Test
    void startAndWaitOrchestration_failedOrchestration() throws Exception {
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

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        final DurableClientContext durableContext = mock(DurableClientContext.class);
        final DurableTaskClient client = mock(DurableTaskClient.class);
        final String scheduleNewOrchestrationInstance = "scheduleNewOrchestrationInstance";
        doReturn(client).when(durableContext).getClient();
        doReturn(scheduleNewOrchestrationInstance).when(client).scheduleNewOrchestrationInstance("Onboardings", onboardingId);

        // Invoke
        HttpResponseMessage responseMessage = function.startOrchestration(req, durableContext, context);

        // Verify
        verify(client, times(1))
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

        verify(service, times(1))
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
        verify(orchestrationContext, times(3))
                .callActivity(captorActivity.capture(), any(), any(), any());
        assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(0));
        assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
        assertEquals(SEND_MAIL_REGISTRATION_FOR_CONTRACT, captorActivity.getAllValues().get(2));

        verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.PENDING);
    }

    @Test
    void onboardingOrchestratorContractRegistrationAggregator() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.REQUEST);
        onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION_AGGREGATOR);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        verify(orchestrationContext, times(4))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_AGGREGATES_CSV_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
        assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(2));
        assertEquals(SEND_MAIL_REGISTRATION_FOR_CONTRACT, captorActivity.getAllValues().get(3));

        verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.PENDING);

        function.onboardingsOrchestrator(orchestrationContext, executionContext);
    }

    @Test
    void onboardingOrchestratorContractRegistrationAggregator_Pending() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.PENDING);
        AggregateInstitution aggregate1 = new AggregateInstitution();
        aggregate1.setTaxCode("code1");
        AggregateInstitution aggregate2 = new AggregateInstitution();
        aggregate1.setTaxCode("code2");
        AggregateInstitution aggregate3 = new AggregateInstitution();
        aggregate1.setTaxCode("code3");
        onboarding.setAggregates(List.of(aggregate1, aggregate2, aggregate3));
        Institution institution = new Institution();
        institution.setId("id");
        onboarding.setInstitution(institution);
        onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION_AGGREGATOR);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(6))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
        assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
        assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
        assertEquals(REJECT_OUTDATED_ONBOARDINGS, captorActivity.getAllValues().get(4));
        assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(5));

        Mockito.verify(orchestrationContext, times(3))
                .callSubOrchestrator(eq(ONBOARDINGS_AGGREGATE_ORCHESTRATOR), any(), any());

        Mockito.verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);

        function.onboardingsOrchestrator(orchestrationContext, executionContext);
    }

    @Test
    void onboardingOrchestratorIncrementRegistrationAggregator_Pending_delgationAlreadyExists(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.PENDING);
        AggregateInstitution aggregate1 = new AggregateInstitution();
        aggregate1.setTaxCode("code1");
        AggregateInstitution aggregate2 = new AggregateInstitution();
        aggregate2.setTaxCode("code2");
        onboarding.setAggregates(List.of(aggregate1, aggregate2));
        Institution institution = new Institution();
        institution.setId("id");
        onboarding.setInstitution(institution);
        onboarding.setWorkflowType(WorkflowType.INCREMENT_REGISTRATION_AGGREGATOR);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContextForIncrementAggregator(onboarding, "true");
        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        Mockito.verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
    }

    @Test
    void onboardingOrchestratorIncrementRegistrationAggregator_Pending_delgationNotExists(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.PENDING);
        AggregateInstitution aggregate1 = new AggregateInstitution();
        aggregate1.setTaxCode("code1");
        AggregateInstitution aggregate2 = new AggregateInstitution();
        aggregate2.setTaxCode("code2");
        onboarding.setAggregates(List.of(aggregate1, aggregate2));
        Institution institution = new Institution();
        institution.setId("id");
        onboarding.setInstitution(institution);
        onboarding.setWorkflowType(WorkflowType.INCREMENT_REGISTRATION_AGGREGATOR);
        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContextForIncrementAggregator(onboarding, "false");

        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        Mockito.verify(orchestrationContext, times(2))
                .callSubOrchestrator(eq(ONBOARDINGS_AGGREGATE_ORCHESTRATOR), any(), any());

        Mockito.verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
    }





    @Test
    void onboardingsOrchestratorNewAdminRequest() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.REQUEST);
        onboarding.setWorkflowType(WorkflowType.USERS);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        verify(orchestrationContext, times(3))
                .callActivity(captorActivity.capture(), any(), any(), any());
        assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(0));
        assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
        assertEquals(SEND_MAIL_REGISTRATION_FOR_CONTRACT, captorActivity.getAllValues().get(2));

        verify(service, times(1))
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
        verify(orchestrationContext, times(1))
                .callActivity(captorActivity.capture(), any(), any(), any());
        assertEquals(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY, captorActivity.getAllValues().get(0));

        verify(service, times(1))
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
        verify(orchestrationContext, times(3))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(0));
        assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
        assertEquals(SEND_MAIL_REGISTRATION_FOR_CONTRACT_WHEN_APPROVE_ACTIVITY, captorActivity.getAllValues().get(2));

        verify(service, times(1))
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
        verify(orchestrationContext, times(6))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
        assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
        assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
        assertEquals(REJECT_OUTDATED_ONBOARDINGS, captorActivity.getAllValues().get(4));
        assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(5));

        verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
    }

    @Test
    void onboardingsOrchestratorConfirmationWithTestProductIds() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.PENDING);
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION);
        onboarding.setInstitution(new Institution());
        onboarding.setTestEnvProductIds(List.of("test1"));

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        verify(orchestrationContext, times(8))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
        assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
        assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
        assertEquals(REJECT_OUTDATED_ONBOARDINGS, captorActivity.getAllValues().get(4));
        assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(5));
        assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(6));
        assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(7));

        verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
    }

    @Test
    void onboardingOrchestratorConfirmAggregate(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.PENDING);
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATE);
        Institution institution = new Institution();
        institution.setId("id");
        onboarding.setInstitution(institution);

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        Mockito.verify(orchestrationContext, times(6))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
        assertEquals(CREATE_DELEGATION_ACTIVITY, captorActivity.getAllValues().get(2));
        assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(3));
        assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(4));
        assertEquals(SEND_MAIL_COMPLETION_AGGREGATE_ACTIVITY, captorActivity.getAllValues().get(5));

        Mockito.verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);

        function.onboardingsOrchestrator(orchestrationContext, executionContext);
    }

    @Test
    void onboardingsAggregateOrchestrator(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.PENDING);
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATE);
        onboarding.setInstitution(new Institution());
        onboarding.setTestEnvProductIds(List.of("test1"));

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
        Task<Object> mockTask = Mockito.mock(Task.class);
        when(orchestrationContext.callSubOrchestrator(any(), any(), any()))
                .thenReturn(mockTask);

        function.onboardingsAggregateOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        verify(orchestrationContext, times(1))
                .callActivity(captorActivity.capture(), any(), any(),any());
        verify(orchestrationContext, times(1))
                .callSubOrchestrator(eq("Onboardings"), any(), any());
        assertEquals(EXISTS_DELEGATION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_AGGREGATE_ONBOARDING_REQUEST_ACTIVITY, captorActivity.getAllValues().get(1));

    }
    @Test
    void onboardingsAggregateOrchestrator_resourceNotFound(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.PENDING);
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATE);
        onboarding.setInstitution(new Institution());
        onboarding.setTestEnvProductIds(List.of("test1"));

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);


        when(orchestrationContext.callActivity(any(), any(), any(), any())).thenThrow(ResourceNotFoundException.class);

        assertThrows(ResourceNotFoundException.class, () -> function.onboardingsAggregateOrchestrator(orchestrationContext, executionContext));


        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        verify(orchestrationContext, times(1))
                .callActivity(captorActivity.capture(), any(), any(),any());

        assertEquals(EXISTS_DELEGATION_ACTIVITY, captorActivity.getAllValues().get(0));
        verify(service, times(1)).updateOnboardingStatusAndInstanceId(null, OnboardingStatus.FAILED, orchestrationContext.getInstanceId());
    }

    @Test
    void onboardingsAggregateOrchestrator_taskFailed(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.PENDING);
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATE);
        onboarding.setInstitution(new Institution());
        onboarding.setTestEnvProductIds(List.of("test1"));

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);


        when(orchestrationContext.callActivity(any(), any(), any(), any())).thenThrow(TaskFailedException.class);

        assertThrows(TaskFailedException.class, () -> function.onboardingsAggregateOrchestrator(orchestrationContext, executionContext));


        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        verify(orchestrationContext, times(1))
                .callActivity(captorActivity.capture(), any(), any(),any());

        assertEquals(EXISTS_DELEGATION_ACTIVITY, captorActivity.getAllValues().get(0));
        verify(service, times(1)).updateOnboardingStatusAndInstanceId(null, OnboardingStatus.FAILED, orchestrationContext.getInstanceId());


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
        verify(orchestrationContext, times(4))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
        assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
        // assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
        assertEquals(REJECT_OUTDATED_ONBOARDINGS, captorActivity.getAllValues().get(3));

        verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
    }

    @Test
    void onboardingsOrchestratorNewAdmin() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.PENDING);
        onboarding.setWorkflowType(WorkflowType.USERS);
        onboarding.setInstitution(new Institution());

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        verify(orchestrationContext, times(3))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(1));
        assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(2));

        verify(service, times(1))
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
        verify(orchestrationContext, times(2))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY, captorActivity.getAllValues().get(1));

        verify(service, times(1))
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
        verify(orchestrationContext, times(5))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
        assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
        assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
        assertEquals(REJECT_OUTDATED_ONBOARDINGS, captorActivity.getAllValues().get(4));
        assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(5));

        verify(service, times(1))
                .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
    }

    TaskOrchestrationContext mockTaskOrchestrationContext(Onboarding onboarding) {
        TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
        when(orchestrationContext.getInput(String.class)).thenReturn(onboarding.getId());
        when(service.getOnboarding(anyString())).thenReturn(Optional.of(onboarding));
        when(completionService.existsDelegation(any())).thenReturn("false");

        Task task = mock(Task.class);
        when(orchestrationContext.callActivity(any(),any(),any(),any())).thenReturn(task);
        when(orchestrationContext.callSubOrchestrator(any(),any())).thenReturn(task);
        when(task.await()).thenReturn("false");
        when(orchestrationContext.allOf(anyList())).thenReturn(task);
        return orchestrationContext;
    }

    TaskOrchestrationContext mockTaskOrchestrationContextForIncrementAggregator(Onboarding onboarding, String returnValue) {
        TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
        when(orchestrationContext.getInput(String.class)).thenReturn(onboarding.getId());
        when(service.getOnboarding(onboarding.getId())).thenReturn(Optional.of(onboarding));
        when(completionService.existsDelegation(any())).thenReturn("true");

        Task task = mock(Task.class);
        when(orchestrationContext.callActivity(any(),any(),any(),any())).thenReturn(task);
        when(orchestrationContext.callSubOrchestrator(any(),any())).thenReturn(task);
        when(task.await()).thenReturn(returnValue);
        when(orchestrationContext.allOf(anyList())).thenReturn(task);
        return orchestrationContext;
    }

    @Test
    void buildContract() {

        doNothing().when(service).createContract(any());

        function.buildContract(onboardingWorkflowString, executionContext);

        verify(service, times(1))
                .createContract(any());
    }

    @Test
    void saveToken() {

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).saveTokenWithContract(any());

        function.saveToken(onboardingWorkflowString, executionContext);

        verify(service, times(1))
                .saveTokenWithContract(any());
    }

    @Test
    void sendMailRegistrationWithContract() {

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailRegistrationForContract(any());

        function.sendMailRegistrationForContract(onboardingWorkflowString, executionContext);

        verify(service, times(1))
                .sendMailRegistrationForContract(any());
    }

    @Test
    void sendMailRegistration() {

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailRegistration(any());

        function.sendMailRegistration(onboardinString, executionContext);

        verify(service, times(1))
                .sendMailRegistration(any());
    }

    @Test
    void sendMailRegistrationApprove() {

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailRegistrationApprove(any());

        function.sendMailRegistrationApprove(onboardinString, executionContext);

        verify(service, times(1))
                .sendMailRegistrationApprove(any());
    }

    @Test
    void sendMailOnboardingApprove() {

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailOnboardingApprove(any());

        function.sendMailOnboardingApprove(onboardinString, executionContext);

        verify(service, times(1))
                .sendMailOnboardingApprove(any());
    }

    @Test
    void sendMailRegistrationWithContractWhenApprove() {

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(service).sendMailRegistrationForContractWhenApprove(any());

        function.sendMailRegistrationForContractWhenApprove(onboardingWorkflowString, executionContext);

        verify(service, times(1))
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
        verify(orchestrationContext, times(5))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
        assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
        assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
        assertEquals(REJECT_OUTDATED_ONBOARDINGS, captorActivity.getAllValues().get(4));
        assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(5));

        verify(service, times(1))
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
        verify(orchestrationContext, times(1))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(SEND_MAIL_REJECTION_ACTIVITY, captorActivity.getAllValues().get(0));
    }

    @Test
    void usersPgOrchestrator_whenStatusPending() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.PENDING);
        onboarding.setWorkflowType(WorkflowType.USERS_PG);
        onboarding.setInstitution(new Institution());

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);


        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        verify(orchestrationContext, times(4))
                .callActivity(captorActivity.capture(), any(), any(),any());
        assertEquals(DELETE_MANAGERS_BY_IC_AND_ADE, captorActivity.getAllValues().get(0));
        assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(1));
        assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(2));
    }

    @Test
    void usersPgOrchestrator_whenStatusRequest() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboardingId");
        onboarding.setStatus(OnboardingStatus.REQUEST);
        onboarding.setWorkflowType(WorkflowType.USERS_PG);
        onboarding.setInstitution(new Institution());

        TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);


        function.onboardingsOrchestrator(orchestrationContext, executionContext);

        ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
        verify(orchestrationContext, times(0))
                .callActivity(captorActivity.capture(), any(), any(),any());
    }

    @Test
    void createInstitutionAndPersistInstitutionId() {

        final String institutionId = "institutionId";
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        when(completionService.createInstitutionAndPersistInstitutionId(any()))
                .thenReturn(institutionId);

        String actualInstitutionId = function.createInstitutionAndPersistInstitutionId(onboardinString, executionContext);

        assertEquals(institutionId, actualInstitutionId);
        verify(completionService, times(1))
                .createInstitutionAndPersistInstitutionId(any());
    }

    @Test
    void rejectOutdatingOnboardings() {

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(completionService).rejectOutdatedOnboardings(any());

        function.rejectOutdatedOnboardings(onboardinString, executionContext);

        verify(completionService, times(1))
                .rejectOutdatedOnboardings(any());
    }

    @Test
    void createOnboarding() {

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(completionService).persistOnboarding(any());

        function.createOnboarding(onboardinString, executionContext);

        verify(completionService, times(1))
                .persistOnboarding(any());
    }

    @Test
    void sendCompletedEmail() {

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(completionService).sendCompletedEmail(any());

        function.sendMailCompletion(onboardingWorkflowString, executionContext);

        verify(completionService, times(1))
                .sendCompletedEmail(any());
    }

    @Test
    void sendMailRejection() {

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(completionService).sendMailRejection(any(), any());

        function.sendMailRejection(onboardinString, executionContext);

        verify(completionService, times(1))
                .sendMailRejection(any(), any());
    }

    @Test
    void sendCompletedEmailAggregate() {

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(completionService).sendCompletedEmailAggregate(any());

        function.sendMailCompletionAggregate(onboardinString, executionContext);

        verify(completionService, times(1))
                .sendCompletedEmailAggregate(any());
    }


    @Test
    void createUsersOnboarding() {

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(completionService).persistUsers(any());

        function.createOnboardedUsers(onboardinString, executionContext);

        verify(completionService, times(1))
                .persistUsers(any());
    }

    @Test
    void createAggregateOnboardingRequest() {
        final String onboardingAggregateOrchestratorInputString = "{\"productId\":\"prod-io\"}";

        String onboardingId = "id";
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        when(completionService.createAggregateOnboardingRequest(any())).thenReturn(onboardingId);

        String response = function.createAggregateOnboardingRequest(onboardingAggregateOrchestratorInputString, executionContext);

        Assertions.assertEquals(onboardingId, response);
        Mockito.verify(completionService, times(1))
                .createAggregateOnboardingRequest(any());
    }
    @Test
    void createDelegationForAggregation() {
        final String onboardingString = "{\"onboardingId\":\"onboardingId\"}";

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        when(completionService.createDelegation(any())).thenReturn("delegationId");

        String delegationId = function.createDelegationForAggregation(onboardingString, executionContext);

        Assertions.assertEquals("delegationId", delegationId);
        verify(completionService, times(1))
                .createDelegation(any());
    }

    @Test
    void createDelegationForAggregationIncrement() {
        final String onboardingString = "{\"onboardingId\":\"onboardingId\"}";

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        when(completionService.existsDelegation(any())).thenReturn("true");

        String exists = function.existsDelegation(onboardingString, executionContext);

        Assertions.assertEquals("true", exists);
        verify(completionService, times(1))
                .existsDelegation(any());
    }

    @Test
    void sendTestEmail() {
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(completionService).sendTestEmail(executionContext);

        function.sendTestEmail(req, executionContext);

        verify(completionService, times(1))
                .sendTestEmail(executionContext);
    }

    @Test
    void deleteOldPgManagers() {
        final String onboardingString = "{\"onboardingId\":\"onboardingId\"}";

        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
        doNothing().when(completionService).deleteOldPgManagers(any());

        function.deleteOldPgManagers(onboardingString, executionContext);

        verify(completionService, times(1))
                .deleteOldPgManagers(any());
    }
}