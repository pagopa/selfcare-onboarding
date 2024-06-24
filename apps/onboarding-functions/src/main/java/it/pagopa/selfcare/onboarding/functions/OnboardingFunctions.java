package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.durabletask.*;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import com.microsoft.durabletask.azurefunctions.DurableClientInput;
import com.microsoft.durabletask.azurefunctions.DurableOrchestrationTrigger;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.config.RetryPolicyConfig;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.CompletionService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.onboarding.workflow.*;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static it.pagopa.selfcare.onboarding.functions.CommonFunctions.FORMAT_LOGGER_ONBOARDING_STRING;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.*;

/**
 * Azure Functions with HTTP Trigger integrated with Quarkus
 */
public class OnboardingFunctions {

    public static final String CREATED_NEW_ONBOARDING_ORCHESTRATION_WITH_INSTANCE_ID_MSG = "Created new Onboarding orchestration with instance ID = ";

    private final OnboardingService service;
    private final CompletionService completionService;
    private final ObjectMapper objectMapper;
    private final TaskOptions optionsRetry;

    public OnboardingFunctions(OnboardingService service,
                               ObjectMapper objectMapper,
                               RetryPolicyConfig retryPolicyConfig,
                               CompletionService completionService) {
        this.service = service;
        this.objectMapper = objectMapper;
        this.completionService = completionService;
        final int maxAttempts = retryPolicyConfig.maxAttempts();
        final Duration firstRetryInterval = Duration.ofSeconds(retryPolicyConfig.firstRetryInterval());
        RetryPolicy retryPolicy = new RetryPolicy(maxAttempts, firstRetryInterval);
        retryPolicy.setBackoffCoefficient(retryPolicyConfig.backoffCoefficient());
        optionsRetry = new TaskOptions(retryPolicy);
    }

    /**
     * This HTTP-triggered function starts the orchestration.
     * Depending on the time required to get the response from the orchestration instance, there are two cases:
     * * The orchestration instances complete within the defined timeout and the response is the actual orchestration instance output, delivered synchronously.
     * * The orchestration instances can't complete within the defined timeout, and the response is the default one described in http api uri
     */
    @FunctionName("StartOnboardingOrchestration")
    public HttpResponseMessage startOrchestration(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @DurableClientInput(name = "durableContext") DurableClientContext durableContext,
            final ExecutionContext context) {
        context.getLogger().info("StartOnboardingOrchestration trigger processed a request.");

        final String onboardingId = request.getQueryParameters().get("onboardingId");
        final String timeoutString = request.getQueryParameters().get("timeout");

        DurableTaskClient client = durableContext.getClient();
        String instanceId = client.scheduleNewOrchestrationInstance("Onboardings", onboardingId);
        context.getLogger().info(String.format("%s %s", CREATED_NEW_ONBOARDING_ORCHESTRATION_WITH_INSTANCE_ID_MSG, instanceId));

        try {

            /* if timeout is null, caller wants response asynchronously */
            if(Objects.isNull(timeoutString)) {
                return durableContext.createCheckStatusResponse(request, instanceId);
            }

            int timeoutInSeconds = Integer.parseInt(timeoutString);
            OrchestrationMetadata metadata = client.waitForInstanceCompletion(
                    instanceId,
                    Duration.ofSeconds(timeoutInSeconds),
                    true);

            boolean isFailed = Optional.ofNullable(metadata)
                    .map(orchestration -> OrchestrationRuntimeStatus.FAILED.equals(orchestration.getRuntimeStatus()) )
                    .orElse(true);

            return isFailed
                    ?  request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build()
                    : request.createResponseBuilder(HttpStatus.OK)
                    .build();
        } catch (TimeoutException timeoutEx) {
            // timeout expired - return a 202 response
            return durableContext.createCheckStatusResponse(request, instanceId);
        }
    }

    /**
     * This is the orchestrator function, which can schedule activity functions, create durable timers,
     * or wait for external events in a way that's completely fault-tolerant.
     */
    @FunctionName("Onboardings")
    public void onboardingsOrchestrator(
            @DurableOrchestrationTrigger(name = "taskOrchestrationContext") TaskOrchestrationContext ctx,
            ExecutionContext functionContext) {
        String onboardingId = ctx.getInput(String.class);
        Onboarding onboarding;

        WorkflowExecutor workflowExecutor;

        try {
            onboarding = service.getOnboarding(onboardingId)
                    .orElseThrow(() -> new ResourceNotFoundException(String.format("Onboarding with id %s not found!", onboardingId)));

            switch (onboarding.getWorkflowType()) {
                case CONTRACT_REGISTRATION -> workflowExecutor = new WorkflowExecutorContractRegistration(objectMapper, optionsRetry);
                case CONTRACT_REGISTRATION_AGGREGATOR -> workflowExecutor = new WorkflowExecutorContractRegistrationAggregator(objectMapper, optionsRetry);
                case FOR_APPROVE ->  workflowExecutor = new WorkflowExecutorForApprove(objectMapper, optionsRetry);
                case FOR_APPROVE_PT -> workflowExecutor = new WorkflowExecutorForApprovePt(objectMapper, optionsRetry);
                case CONFIRMATION -> workflowExecutor = new WorkflowExecutorConfirmation(objectMapper, optionsRetry);
                case IMPORT -> workflowExecutor = new WorkflowExecutorImport(objectMapper, optionsRetry);
                case USERS -> workflowExecutor = new WorkflowExecutorForUsers(objectMapper, optionsRetry);
                default -> throw new IllegalArgumentException("Workflow options not found!");
            }

            Optional<OnboardingStatus> optNextStatus = workflowExecutor.execute(ctx, onboarding);
            optNextStatus.ifPresent(onboardingStatus -> service.updateOnboardingStatus(onboardingId, onboardingStatus));
        } catch (TaskFailedException ex) {
            functionContext.getLogger().warning("Error during workflowExecutor execute, msg: " + ex.getMessage());
            service.updateOnboardingStatusAndInstanceId(onboardingId, OnboardingStatus.FAILED, ctx.getInstanceId());
            throw ex;
        } catch (ResourceNotFoundException ex) {
            functionContext.getLogger().warning(ex.getMessage());
            service.updateOnboardingStatusAndInstanceId(onboardingId, OnboardingStatus.FAILED, ctx.getInstanceId());
            throw ex;
        }
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName(BUILD_CONTRACT_ACTIVITY_NAME)
    public void buildContract(@DurableActivityTrigger(name = "onboardingString") String onboardingWorkflowString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, BUILD_CONTRACT_ACTIVITY_NAME, onboardingWorkflowString));
        service.createContract(readOnboardingWorkflowValue(objectMapper, onboardingWorkflowString));
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME)
    public void saveToken(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingString));
        service.saveTokenWithContract(readOnboardingValue(objectMapper, onboardingString));
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName(SEND_MAIL_REGISTRATION_FOR_CONTRACT)
    public void sendMailRegistrationForContract(@DurableActivityTrigger(name = "onboardingString") String onboardingWorkflowString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REGISTRATION_FOR_CONTRACT, onboardingWorkflowString));
        service.sendMailRegistrationForContract(readOnboardingWorkflowValue(objectMapper, onboardingWorkflowString));
    }

    @FunctionName(SEND_MAIL_REGISTRATION_FOR_CONTRACT_WHEN_APPROVE_ACTIVITY)
    public void sendMailRegistrationForContractWhenApprove(@DurableActivityTrigger(name = "onboardingString") String onboardingWorkflowString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REGISTRATION_FOR_CONTRACT_WHEN_APPROVE_ACTIVITY, onboardingWorkflowString));
        service.sendMailRegistrationForContractWhenApprove(readOnboardingWorkflowValue(objectMapper, onboardingWorkflowString));
    }

    @FunctionName(SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY)
    public void sendMailRegistration(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY, onboardingString));
        service.sendMailRegistration(readOnboardingValue(objectMapper, onboardingString));
    }

    @FunctionName(SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY)
    public void sendMailRegistrationApprove(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY, onboardingString));
        service.sendMailRegistrationApprove(readOnboardingValue(objectMapper, onboardingString));
    }

    @FunctionName(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY)
    public void sendMailOnboardingApprove(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY, onboardingString));
        service.sendMailOnboardingApprove(readOnboardingValue(objectMapper, onboardingString));
    }

    @FunctionName(CREATE_INSTITUTION_ACTIVITY)
    public String createInstitutionAndPersistInstitutionId(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, CREATE_INSTITUTION_ACTIVITY, onboardingString));
        return completionService.createInstitutionAndPersistInstitutionId(readOnboardingValue(objectMapper, onboardingString));
    }

    @FunctionName(CREATE_ONBOARDING_ACTIVITY)
    public void createOnboarding(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, CREATE_ONBOARDING_ACTIVITY, onboardingString));
        completionService.persistOnboarding(readOnboardingValue(objectMapper, onboardingString));
    }

    @FunctionName(STORE_ONBOARDING_ACTIVATEDAT)
    public void storeOnboardingActivatedAt(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, STORE_ONBOARDING_ACTIVATEDAT, onboardingString));
        completionService.persistActivatedAt(readOnboardingValue(objectMapper, onboardingString));
    }

    @FunctionName(SEND_MAIL_COMPLETION_ACTIVITY)
    public void sendMailCompletion(@DurableActivityTrigger(name = "onboardingString") String onboardingWorkflowString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_COMPLETION_ACTIVITY, onboardingWorkflowString));
        completionService.sendCompletedEmail(readOnboardingWorkflowValue(objectMapper, onboardingWorkflowString));
    }

    @FunctionName(SEND_MAIL_REJECTION_ACTIVITY)
    public void sendMailRejection(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REJECTION_ACTIVITY, onboardingString));
        completionService.sendMailRejection(readOnboardingValue(objectMapper, onboardingString));
    }

    @FunctionName(CREATE_USERS_ACTIVITY)
    public void createOnboardedUsers(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, CREATE_USERS_ACTIVITY, onboardingString));
        completionService.persistUsers(readOnboardingValue(objectMapper, onboardingString));
    }

    @FunctionName(SEND_MAIL_COMPLETION_AGGREGATE_ACTIVITY)
    public void sendMailCompletionAggregate(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_COMPLETION_AGGREGATE_ACTIVITY, onboardingString));
        completionService.sendCompletedEmailAggregate(readOnboardingValue(objectMapper, onboardingString));
    }

    @FunctionName(CREATE_AGGREGATE_ONBOARDING_REQUEST_ACTIVITY)
    public Onboarding createAggregateOnboardingRequest(@DurableActivityTrigger(name = "onboardingString") String onboardingAggregateOrchestratorInputString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, CREATE_AGGREGATE_ONBOARDING_REQUEST_ACTIVITY, onboardingAggregateOrchestratorInputString));
        return completionService.createAggregateOnboardingRequest(readOnboardingAggregateOrchestratorInputValue(objectMapper, onboardingAggregateOrchestratorInputString));
    }
    @FunctionName(CREATE_DELEGATION_ACTIVITY)
    public void createDelegationForAggregation(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, CREATE_USERS_ACTIVITY, onboardingString));
        completionService.createDelegation(readOnboardingValue(objectMapper, onboardingString));
    }
}
