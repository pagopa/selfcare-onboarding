package it.pagopa.selfcare.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import com.microsoft.durabletask.azurefunctions.DurableClientInput;
import com.microsoft.durabletask.azurefunctions.DurableOrchestrationTrigger;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.OnboardingService;

import java.util.Optional;

import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;

public class OnboardingCompletionFunctions {

    public static final String CREATED_NEW_ONBOARDING_COMPLETION_ORCHESTRATION_WITH_INSTANCE_ID_MSG = "Created new Onboarding completion orchestration with instance ID = ";

    public static final String ONBOARDING_COMPLETION_ACTIVITY = "OnboardingCompletion";
    public static final String CREATE_INSTITUTION_ACTIVITY = "CreateInstitution";
    public static final String CREATE_ONBOARDING_ACTIVITY = "CreateOnboarding";
    public static final String SEND_MAIL_COMPLETION_ACTIVITY = "SendMailCompletion";
    private final OnboardingService service;

    private final ObjectMapper objectMapper;

    public OnboardingCompletionFunctions(OnboardingService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    /**
     * This HTTP-triggered function starts the orchestration.
     */
    @FunctionName("StartOnboardingCompletionOrchestration")
    public HttpResponseMessage startOrchestration(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            @DurableClientInput(name = "durableContext") DurableClientContext durableContext,
            final ExecutionContext context) {
        context.getLogger().info("StartOnboardingCompletionOrchestration trigger processed a request.");

        final String onboardingId = request.getQueryParameters().get("onboardingId");

        DurableTaskClient client = durableContext.getClient();
        String instanceId = client.scheduleNewOrchestrationInstance(ONBOARDING_COMPLETION_ACTIVITY, onboardingId);
        context.getLogger().info(String.format("%s %s", CREATED_NEW_ONBOARDING_COMPLETION_ORCHESTRATION_WITH_INSTANCE_ID_MSG, instanceId));

        return durableContext.createCheckStatusResponse(request, instanceId);
    }

    /**
     * This is the orchestrator function, which can schedule activity functions, create durable timers,
     * or wait for external events in a way that's completely fault-tolerant.
     */
    @FunctionName(ONBOARDING_COMPLETION_ACTIVITY)
    public void onboardingCompletionOrchestrator(
            @DurableOrchestrationTrigger(name = "taskOrchestrationContext") TaskOrchestrationContext ctx) {
        String onboardingId = ctx.getInput(String.class);
        Onboarding onboarding = service.getOnboarding(onboardingId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Onboarding with id %s not found!", onboardingId)));
        String onboardingString = getOnboardingString(objectMapper, onboarding);

        //ctx.callActivity(BUILD_CONTRACT_ACTIVITY_NAME, onboardingString, optionsRetry, String.class).await();
        //ctx.callActivity(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingString, optionsRetry, String.class).await();
        //ctx.callActivity(SEND_MAIL_REGISTRATION_WITH_CONTRACT_ACTIVITY, onboardingString, optionsRetry, String.class).await() ;
    }
}
