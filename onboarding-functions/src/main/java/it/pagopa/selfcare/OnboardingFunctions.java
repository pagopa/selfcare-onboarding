package it.pagopa.selfcare;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import com.microsoft.durabletask.azurefunctions.DurableClientInput;
import com.microsoft.durabletask.azurefunctions.DurableOrchestrationTrigger;
import jakarta.inject.Inject;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger integrated with Quarkus
 */
public class OnboardingFunctions {
    @Inject
    OnboardingService service;

    /**
     * This HTTP-triggered function starts the orchestration.
     */
    @FunctionName("StartOnboardingOrchestration")
    public HttpResponseMessage startOrchestration(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
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
        return onboardingsOrchestratorDefault(ctx, onboardingId);
    }

    private String onboardingsOrchestratorDefault(TaskOrchestrationContext ctx, String onboardingId){
        String result = "";
        result += ctx.callActivity("BuildContract", onboardingId, String.class).await() + ", ";
        result += ctx.callActivity("SaveInstitutionAndUsers", onboardingId, String.class).await() + ", ";
        result += ctx.callActivity("SendMailRegistration", onboardingId, String.class).await() + ", ";
        return result;
    }

    private String onboardingsOrchestratorPAorSAorGSPIPA(TaskOrchestrationContext ctx){
        String result = "";
        result += ctx.callActivity("BuildContract", "Tokyo", String.class).await() + ", ";
        result += ctx.callActivity("SaveInstitutionAndUsers", "London", String.class).await() + ", ";
        result += ctx.callActivity("SendMailRegistrationWithContract", "Seattle", String.class).await() + ", ";
        return result;
    }

    private String onboardingsOrchestratorPG(TaskOrchestrationContext ctx){
        String result = "";
        result += ctx.callActivity("SaveInstitutionAndUsers", "London", String.class).await() + ", ";
        result += ctx.callActivity("SendMailConfirmation", "Seattle", String.class).await() + ", ";
        return result;
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName("BuildContract")
    public String buildContract(@DurableActivityTrigger(name = "onboardingId") String onboardingId, final ExecutionContext context) {
        context.getLogger().info("BuildContract: " + onboardingId);
        return onboardingId.toUpperCase();
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName("SaveInstitutionAndUsers")
    public String saveInstitutionAndUsers(@DurableActivityTrigger(name = "onboardingId") String onboardingId, final ExecutionContext context) {
        context.getLogger().info("SaveInstitutionAndUsers: " + onboardingId);
        return onboardingId.toUpperCase();
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName("SendMailRegistrationWithContract")
    public String sendMailWithContract(@DurableActivityTrigger(name = "onboardingId") String onboardingId, final ExecutionContext context) {
        context.getLogger().info("SendMailRegistrationWithContract: " + onboardingId);
        return onboardingId.toUpperCase();
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName("SendMailRegistration")
    public String sendMailRegistration(@DurableActivityTrigger(name = "onboardingId") String onboardingId, final ExecutionContext context) {
        context.getLogger().info("SendMailRegistration: " + onboardingId);
        return onboardingId.toUpperCase();
    }

    /**
     * This is the activity function that gets invoked by the orchestrator function.
     */
    @FunctionName("SendMailConfirmation")
    public String sendMailConfirmation(@DurableActivityTrigger(name = "onboardingId") String onboardingId, final ExecutionContext context) {
        context.getLogger().info("SendMailConfirmation: " + onboardingId);
        return onboardingId.toUpperCase();
    }
}
