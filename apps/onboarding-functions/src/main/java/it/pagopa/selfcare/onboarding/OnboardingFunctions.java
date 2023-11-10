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
import jakarta.inject.Inject;

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
    public static final String SEND_MAIL_REGISTRATION_WITH_CONTRACT_ACTIVITY_NAME = "SendMailRegistrationWithContract";
    public static final String SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY_NAME = "SendMailRegistrationRequest";
    public static final String SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY_NAME = "SendMailRegistrationApprove";
    public static final String SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY_NAME = "SendMailOnboardingApprove";
    @Inject
    OnboardingService service;

    @Inject
    ObjectMapper objectMapper;

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
    public String onboardingsOrchestrator(
            @DurableOrchestrationTrigger(name = "taskOrchestrationContext") TaskOrchestrationContext ctx) {
        String onboardingId = ctx.getInput(String.class);
        String onboardingString = getOnboardingString(onboardingId);

        return onboardingsOrchestratorDefault(ctx, onboardingString);
    }

    private String getOnboardingString(String onboardingId) {
        Onboarding onboarding = service.getOnboarding(onboardingId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Onboarding with id %s not found!", onboardingId)));

        String onboardingString;
        try {
            onboardingString = objectMapper.writeValueAsString(onboarding);
        } catch (JsonProcessingException e) {
            throw new FunctionOrchestratedException(e);
        }
        return onboardingString;
    }

    private String onboardingsOrchestratorDefault(TaskOrchestrationContext ctx, String onboardingString){
        String result = "";
        result += ctx.callActivity(BUILD_CONTRACT_ACTIVITY_NAME, onboardingString, optionsRetry, String.class).await() + ", ";
        result += ctx.callActivity(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingString, optionsRetry, String.class).await() + ", ";
        result += ctx.callActivity(SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY_NAME, onboardingString, optionsRetry, String.class).await() + ", ";
        result += ctx.callActivity(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY_NAME, onboardingString, optionsRetry, String.class).await() + ", ";
        return result;
    }

    private String onboardingsOrchestratorPAorSAorGSPIPA(TaskOrchestrationContext ctx, String onboardingString){
        String result = "";
        result += ctx.callActivity(BUILD_CONTRACT_ACTIVITY_NAME, onboardingString, optionsRetry, String.class).await() + ", ";
        result += ctx.callActivity(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingString, optionsRetry, String.class).await() + ", ";
        result += ctx.callActivity(SEND_MAIL_REGISTRATION_WITH_CONTRACT_ACTIVITY_NAME, onboardingString, optionsRetry,  String.class).await() + ", ";
        return result;
    }

    private String onboardingsOrchestratorPG(TaskOrchestrationContext ctx, String onboardingString){
        String result = "";
        result += ctx.callActivity(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingString, optionsRetry,  String.class).await() + ", ";
        result += ctx.callActivity("SendMailConfirmation", onboardingString, optionsRetry,  String.class).await() + ", ";
        return result;
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName(BUILD_CONTRACT_ACTIVITY_NAME)
    public String buildContract(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, BUILD_CONTRACT_ACTIVITY_NAME, onboardingString));
        service.createContract(readOnboardingValue(onboardingString));
        return onboardingString;
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME)
    public String saveToken(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingString));
        service.saveTokenWithContract(readOnboardingValue(onboardingString));
        return onboardingString;
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName(SEND_MAIL_REGISTRATION_WITH_CONTRACT_ACTIVITY_NAME)
    public String sendMailRegistrationWithContract(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REGISTRATION_WITH_CONTRACT_ACTIVITY_NAME, onboardingString));
        service.sendMailRegistrationWithContract(readOnboardingValue(onboardingString));
        return onboardingString;
    }

    @FunctionName(SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY_NAME)
    public String sendMailRegistration(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY_NAME, onboardingString));
        service.sendMailRegistration(readOnboardingValue(onboardingString));
        return onboardingString;
    }

    @FunctionName(SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY_NAME)
    public String sendMailRegistrationApprove(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY_NAME, onboardingString));
        service.sendMailRegistrationApprove(readOnboardingValue(onboardingString));
        return onboardingString;
    }

    @FunctionName(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY_NAME)
    public String sendMailOnboardingApprove(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY_NAME, onboardingString));
        service.sendMailOnboardingApprove(readOnboardingValue(onboardingString));
        return onboardingString;
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName("SendMailConfirmation")
    public String sendMailConfirmation(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING,"SendMailConfirmation", onboardingString));
        return onboardingString;
    }

    private Onboarding readOnboardingValue(String onboardingString) {
        try {
            return objectMapper.readValue(onboardingString, Onboarding.class);
        } catch (JsonProcessingException e) {
            throw new FunctionOrchestratedException(e);
        }
    }
}
