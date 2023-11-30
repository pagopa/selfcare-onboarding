package it.pagopa.selfcare.onboarding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.durabletask.RetryPolicy;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import com.microsoft.durabletask.azurefunctions.DurableClientInput;
import com.microsoft.durabletask.azurefunctions.DurableOrchestrationTrigger;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.FunctionOrchestratedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.OnboardingService;

import java.time.Duration;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger integrated with Quarkus
 */
public class OnboardingFunctions {
    public static final String CREATED_NEW_ONBOARDING_ORCHESTRATION_WITH_INSTANCE_ID_MSG = "Created new Onboarding orchestration with instance ID = ";
    public static final String SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME = "SaveTokenWithContract";
    public static final String BUILD_CONTRACT_ACTIVITY_NAME = "BuildContract";
    public static final String FORMAT_LOGGER_ONBOARDING_STRING = "%s: %s";
    public static final String SEND_MAIL_REGISTRATION_WITH_CONTRACT_ACTIVITY = "SendMailRegistrationWithContract";
    public static final String SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY = "SendMailRegistrationRequest";
    public static final String SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY = "SendMailRegistrationApprove";
    public static final String SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY = "SendMailOnboardingApprove";
    public static final String SEND_MAIL_CONFIRMATION_ACTIVITY = "SendMailConfirmation";

    private final OnboardingService service;

    private final ObjectMapper objectMapper;

    public OnboardingFunctions(OnboardingService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    private static final  TaskOptions optionsRetry;

    static {
        // Make 3 attempts with 5 seconds between retries
        final int maxAttempts = 1;
        final Duration firstRetryInterval = Duration.ofSeconds(3);
        RetryPolicy retryPolicy = new RetryPolicy(maxAttempts, firstRetryInterval);
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
        String onboardingString = getOnboardingString(onboarding);

        switch (onboarding.getWorkflowType()) {
            case CONTRACT_REGISTRATION -> workflowContractRegistration(ctx, onboardingString);
            case FOR_APPROVE -> workflowForApprove(ctx, onboardingString);
            case FOR_APPROVE_PT -> workflowRegistrationRequestAndApprove(ctx, onboardingString);
            case CONFIRMATION -> workflowForConfirmation(ctx, onboardingString);
        }
    }

    private void workflowContractRegistration(TaskOrchestrationContext ctx, String onboardingString){
        ctx.callActivity(BUILD_CONTRACT_ACTIVITY_NAME, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(SEND_MAIL_REGISTRATION_WITH_CONTRACT_ACTIVITY, onboardingString, optionsRetry, String.class).await() ;
    }

    private void workflowForApprove(TaskOrchestrationContext ctx, String onboardingString){
        ctx.callActivity(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY, onboardingString, optionsRetry, String.class).await() ;
    }

    private void workflowRegistrationRequestAndApprove(TaskOrchestrationContext ctx, String onboardingString){
        ctx.callActivity(SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY, onboardingString, optionsRetry, String.class).await();
    }

    private void workflowForConfirmation(TaskOrchestrationContext ctx, String onboardingString){
        ctx.callActivity(SEND_MAIL_CONFIRMATION_ACTIVITY, onboardingString, optionsRetry, String.class).await();
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName(BUILD_CONTRACT_ACTIVITY_NAME)
    public void buildContract(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, BUILD_CONTRACT_ACTIVITY_NAME, onboardingString));
        service.createContract(readOnboardingValue(onboardingString));
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME)
    public void saveToken(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingString));
        service.saveTokenWithContract(readOnboardingValue(onboardingString));
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName(SEND_MAIL_REGISTRATION_WITH_CONTRACT_ACTIVITY)
    public void sendMailRegistrationWithContract(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REGISTRATION_WITH_CONTRACT_ACTIVITY, onboardingString));
        service.sendMailRegistrationWithContract(readOnboardingValue(onboardingString));
    }

    @FunctionName(SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY)
    public void sendMailRegistration(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY, onboardingString));
        service.sendMailRegistration(readOnboardingValue(onboardingString));
    }

    @FunctionName(SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY)
    public void sendMailRegistrationApprove(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY, onboardingString));
        service.sendMailRegistrationApprove(readOnboardingValue(onboardingString));
    }

    @FunctionName(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY)
    public void sendMailOnboardingApprove(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY, onboardingString));
        service.sendMailOnboardingApprove(readOnboardingValue(onboardingString));
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName(SEND_MAIL_CONFIRMATION_ACTIVITY)
    public String sendMailConfirmation(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_CONFIRMATION_ACTIVITY, onboardingString));
        return onboardingString;
    }

    private Onboarding readOnboardingValue(String onboardingString) {
        try {
            return objectMapper.readValue(onboardingString, Onboarding.class);
        } catch (JsonProcessingException e) {
            throw new FunctionOrchestratedException(e);
        }
    }

    private String getOnboardingString(Onboarding onboarding) {

        String onboardingString;
        try {
            onboardingString = objectMapper.writeValueAsString(onboarding);
        } catch (JsonProcessingException e) {
            throw new FunctionOrchestratedException(e);
        }
        return onboardingString;
    }
}
