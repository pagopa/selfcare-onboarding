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
import it.pagopa.selfcare.onboarding.config.RetryPolicyConfig;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.CompletionService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.onboarding.workflow.*;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static it.pagopa.selfcare.onboarding.functions.CommonFunctions.FORMAT_LOGGER_ONBOARDING_STRING;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.readOnboardingValue;

/**
 * Azure Functions with HTTP Trigger integrated with Quarkus
 */
public class OnboardingFunctions {
    public static final String CREATED_NEW_ONBOARDING_ORCHESTRATION_WITH_INSTANCE_ID_MSG = "Created new Onboarding orchestration with instance ID = ";


    private final OnboardingService service;
    private final CompletionService completionService;

    private final ObjectMapper objectMapper;
    private final TaskOptions optionsRetry;

    public OnboardingFunctions(OnboardingService service, ObjectMapper objectMapper, RetryPolicyConfig retryPolicyConfig, CompletionService completionService) {
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
     */
    @FunctionName("StartOnboardingOrchestration")
    public HttpResponseMessage startOrchestration(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @DurableClientInput(name = "durableContext") DurableClientContext durableContext,
            final ExecutionContext context) {
        context.getLogger().info("StartOnboardingOrchestration trigger processed a request.");

        final String onboardingId = request.getQueryParameters().get("onboardingId");

        DurableTaskClient client = durableContext.getClient();
        String instanceId = client.scheduleNewOrchestrationInstance("Onboardings", onboardingId);
        context.getLogger().info(String.format("%s %s", CREATED_NEW_ONBOARDING_ORCHESTRATION_WITH_INSTANCE_ID_MSG, instanceId));

        return durableContext.createCheckStatusResponse(request, instanceId);
    }

    @FunctionName("StartAndWaitOnboardingOrchestration")
    public HttpResponseMessage startAndWaitOrchestration(
            @HttpTrigger(name = "req", route = "StartAndWaitOnboardingOrchestration", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @DurableClientInput(name = "durableContext") DurableClientContext durableContext,
            final ExecutionContext context) {
        context.getLogger().info("StartAndWaitOrchestration trigger processed a request.");

        final String onboardingId = request.getQueryParameters().get("onboardingId");

        DurableTaskClient client = durableContext.getClient();
        String instanceId = client.scheduleNewOrchestrationInstance("Onboardings", onboardingId);
        context.getLogger().info(String.format("%s %s", CREATED_NEW_ONBOARDING_ORCHESTRATION_WITH_INSTANCE_ID_MSG, instanceId));

        try {

            int timeoutInSeconds = 60;
            OrchestrationMetadata orchestration = client.waitForInstanceCompletion(
                    instanceId,
                    Duration.ofSeconds(timeoutInSeconds),
                    true /* getInputsAndOutputs */);
            return request.createResponseBuilder(HttpStatus.OK)
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
            @DurableOrchestrationTrigger(name = "taskOrchestrationContext") TaskOrchestrationContext ctx) {
        String onboardingId = ctx.getInput(String.class);
        Onboarding onboarding = service.getOnboarding(onboardingId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Onboarding with id %s not found!", onboardingId)));

        WorkflowExecutor workflowExecutor;

        switch (onboarding.getWorkflowType()) {
            case CONTRACT_REGISTRATION -> workflowExecutor = new WorkflowExecutorContractRegistration(objectMapper, optionsRetry);
            case FOR_APPROVE ->  workflowExecutor = new WorkflowExecutorForApprove(objectMapper, optionsRetry);
            case FOR_APPROVE_PT -> workflowExecutor = new WorkflowExecutorForApprovePt(objectMapper, optionsRetry);
            case CONFIRMATION -> workflowExecutor = new WorkflowExecutorConfirmation(objectMapper, optionsRetry);
            default -> throw new IllegalArgumentException("Workflow options not found!");
        }

        workflowExecutor.execute(ctx, onboarding);
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName(BUILD_CONTRACT_ACTIVITY_NAME)
    public void buildContract(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, BUILD_CONTRACT_ACTIVITY_NAME, onboardingString));
        service.createContract(readOnboardingValue(objectMapper, onboardingString));
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
    @FunctionName(SEND_MAIL_REGISTRATION_WITH_CONTRACT_ACTIVITY)
    public void sendMailRegistrationWithContract(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REGISTRATION_WITH_CONTRACT_ACTIVITY, onboardingString));
        service.sendMailRegistrationWithContract(readOnboardingValue(objectMapper, onboardingString));
    }
    @FunctionName(SEND_MAIL_REGISTRATION_WITH_CONTRACT_WHEN_APPROVE_ACTIVITY)
    public void sendMailRegistrationWithContractWhenApprove(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REGISTRATION_WITH_CONTRACT_WHEN_APPROVE_ACTIVITY, onboardingString));
        service.sendMailRegistrationWithContractWhenApprove(readOnboardingValue(objectMapper, onboardingString));
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

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName(SEND_MAIL_CONFIRMATION_ACTIVITY)
    public String sendMailConfirmation(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_CONFIRMATION_ACTIVITY, onboardingString));
        return onboardingString;
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

    @FunctionName(SEND_MAIL_COMPLETION_ACTIVITY)
    public void sendMailCompletion(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_COMPLETION_ACTIVITY, onboardingString));
        completionService.sendCompletedEmail(readOnboardingValue(objectMapper, onboardingString));
    }
}
