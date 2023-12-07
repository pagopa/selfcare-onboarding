package it.pagopa.selfcare.onboarding.functions;

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
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.functions.utils.SaveOnboardingStatusInput;
import it.pagopa.selfcare.onboarding.service.CompletionService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;

import java.time.Duration;
import java.util.Optional;

import static it.pagopa.selfcare.onboarding.functions.CommonFunctions.FORMAT_LOGGER_ONBOARDING_STRING;
import static it.pagopa.selfcare.onboarding.functions.CommonFunctions.SAVE_ONBOARDING_STATUS_ACTIVITY;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;
import static it.pagopa.selfcare.onboarding.utils.Utils.readOnboardingValue;

public class OnboardingCompletionFunctions {

    public static final String CREATED_NEW_ONBOARDING_COMPLETION_ORCHESTRATION_WITH_INSTANCE_ID_MSG = "Created new Onboarding completion orchestration with instance ID = ";

    public static final String ONBOARDING_COMPLETION_ACTIVITY = "OnboardingCompletion";
    public static final String CREATE_INSTITUTION_ACTIVITY = "CreateInstitution";
    public static final String CREATE_ONBOARDING_ACTIVITY = "CreateOnboarding";
    public static final String SEND_MAIL_COMPLETION_ACTIVITY = "SendMailCompletion";
    private final OnboardingService service;

    private final CompletionService completionService;

    private final ObjectMapper objectMapper;
    private final TaskOptions optionsRetry;

    public OnboardingCompletionFunctions(OnboardingService service, CompletionService completionService, ObjectMapper objectMapper) {
        this.service = service;
        this.completionService = completionService;
        this.objectMapper = objectMapper;

        final int maxAttempts = 1;
        final Duration firstRetryInterval = Duration.ofSeconds(3);
        RetryPolicy retryPolicy = new RetryPolicy(maxAttempts, firstRetryInterval);
        optionsRetry = new TaskOptions(retryPolicy);
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

        //CreateInstitution activity return an institutionId that is used by CreateOnboarding activity
        String institutionId = ctx.callActivity(CREATE_INSTITUTION_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        onboarding.getInstitution().setId(institutionId);
        onboardingString = getOnboardingString(objectMapper, onboarding);

        ctx.callActivity(CREATE_ONBOARDING_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(SEND_MAIL_COMPLETION_ACTIVITY, onboardingString, optionsRetry, String.class).await();

        //Last activity consist of saving pending status
        String saveOnboardingStatusInput =  SaveOnboardingStatusInput.buildAsJsonString(onboardingId, OnboardingStatus.COMPLETED.name());
        ctx.callActivity(SAVE_ONBOARDING_STATUS_ACTIVITY, saveOnboardingStatusInput, optionsRetry, String.class).await();
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
