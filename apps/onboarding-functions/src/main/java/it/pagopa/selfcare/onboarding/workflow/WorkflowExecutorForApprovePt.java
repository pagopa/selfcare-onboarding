package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;

import static it.pagopa.selfcare.onboarding.functions.CommonFunctions.SAVE_ONBOARDING_STATUS_ACTIVITY;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY;
import static it.pagopa.selfcare.onboarding.functions.utils.SaveOnboardingStatusInput.buildAsJsonString;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;

public class WorkflowExecutorForApprovePt implements WorkflowExecutor {

    private final ObjectMapper objectMapper;
    private final TaskOptions optionsRetry;

    public WorkflowExecutorForApprovePt(ObjectMapper objectMapper, TaskOptions optionsRetry) {
        this.objectMapper = objectMapper;
        this.optionsRetry = optionsRetry;
    }
    @Override
    public void executeRequestState(TaskOrchestrationContext ctx, Onboarding onboarding) {
        String onboardingString = getOnboardingString(objectMapper, onboarding);
        ctx.callActivity(SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(SAVE_ONBOARDING_STATUS_ACTIVITY, buildAsJsonString(onboarding.getOnboardingId(), OnboardingStatus.TOBEVALIDATED.name()), optionsRetry, String.class).await();
    }

    @Override
    public void executeToBeValidatedState(TaskOrchestrationContext ctx, Onboarding onboarding) {
        executePendingState(ctx, onboarding);
    }

    @Override
    public void executePendingState(TaskOrchestrationContext ctx, Onboarding onboarding) {
    }

    @Override
    public ObjectMapper objectMapper() {
        return this.objectMapper;
    }

    @Override
    public TaskOptions optionsRetry() {
        return this.optionsRetry;
    }
}
