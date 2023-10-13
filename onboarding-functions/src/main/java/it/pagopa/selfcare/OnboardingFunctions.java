package it.pagopa.selfcare;

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
import it.pagopa.selfcare.entity.Onboarding;
import it.pagopa.selfcare.service.OnboardingService;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger integrated with Quarkus
 */
public class OnboardingFunctions {
    @Inject
    OnboardingService service;

    @Inject
    ObjectMapper objectMapper;

    final static TaskOptions optionsRetry;

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
        context.getLogger().info("Created new Onboarding orchestration with instance ID = " + instanceId);

        return durableContext.createCheckStatusResponse(request, instanceId);
    }

    /**
     * This is the orchestrator function, which can schedule activity functions, create durable timers,
     * or wait for external events in a way that's completely fault-tolerant.
     */
    @FunctionName("Onboardings")
    public String OnboardingsOrchestrator(
            @DurableOrchestrationTrigger(name = "taskOrchestrationContext") TaskOrchestrationContext ctx) {
        String onboardingId = ctx.getInput(String.class);
        String onboardingString = getOnboardingString(onboardingId);

        return onboardingsOrchestratorDefault(ctx, onboardingString);
    }

    private String getOnboardingString(String onboardingId) {
        Onboarding onboarding = service.getOnboarding(onboardingId);

        String onboardingString = null;
        try {
            onboardingString = objectMapper.writeValueAsString(onboarding);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return onboardingString;
    }

    private String onboardingsOrchestratorDefault(TaskOrchestrationContext ctx, String onboardingString){
        String result = "";
        result += ctx.callActivity("BuildContract", onboardingString, optionsRetry, String.class).await() + ", ";
        result += ctx.callActivity("SaveToken", onboardingString, optionsRetry, String.class).await() + ", ";
        result += ctx.callActivity("SendMailRegistration", onboardingString, optionsRetry, String.class).await() + ", ";
        return result;
    }

    private String onboardingsOrchestratorPAorSAorGSPIPA(TaskOrchestrationContext ctx, String onboardingString){
        String result = "";
        result += ctx.callActivity("BuildContract", onboardingString, optionsRetry, String.class).await() + ", ";
        result += ctx.callActivity("SaveToken", onboardingString, optionsRetry, String.class).await() + ", ";
        result += ctx.callActivity("SendMailRegistrationWithContract", onboardingString, optionsRetry,  String.class).await() + ", ";
        return result;
    }

    private String onboardingsOrchestratorPG(TaskOrchestrationContext ctx, String onboardingString){
        String result = "";
        result += ctx.callActivity("SaveToken", onboardingString, optionsRetry,  String.class).await() + ", ";
        result += ctx.callActivity("SendMailConfirmation", onboardingString, optionsRetry,  String.class).await() + ", ";
        return result;
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName("BuildContract")
    public String buildContract(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info("BuildContract: " + onboardingString);
        try {
            service.createContract(objectMapper.readValue(onboardingString, Onboarding.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return onboardingString;
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName("SaveToken")
    public String SaveToken(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info("SaveToken: " + onboardingString);
        return onboardingString;
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName("SendMailRegistrationWithContract")
    public String sendMailWithContract(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info("SendMailRegistrationWithContract: " + onboardingString);
        return onboardingString;
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName("SendMailRegistration")
    public String sendMailRegistration(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info("SendMailRegistration: " + onboardingString);
        return onboardingString;
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName("SendMailConfirmation")
    public String sendMailConfirmation(@DurableActivityTrigger(name = "onboardingString") String onboardingString, final ExecutionContext context) {
        context.getLogger().info("SendMailConfirmation: " + onboardingString);
        return onboardingString;
    }
}
