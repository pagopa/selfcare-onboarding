package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;

import java.util.Optional;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;

public class WorkflowExecutorForApprove implements WorkflowExecutor {

    private final ObjectMapper objectMapper;
    private final TaskOptions optionsRetry;

    public WorkflowExecutorForApprove(ObjectMapper objectMapper, TaskOptions optionsRetry) {
        this.objectMapper = objectMapper;
        this.optionsRetry = optionsRetry;
    }
    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, Onboarding onboarding) {
        String onboardingString = getOnboardingString(objectMapper, onboarding);
        ctx.callActivity(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        return Optional.of(OnboardingStatus.TOBEVALIDATED);
    }

    @Override
    public Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, Onboarding onboarding) {
        String onboardingString = getOnboardingString(objectMapper, onboarding);
        ctx.callActivity(BUILD_CONTRACT_ACTIVITY_NAME, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(SEND_MAIL_REGISTRATION_FOR_CONTRACT_WHEN_APPROVE_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        return Optional.of(OnboardingStatus.PENDING);
    }

    @Override
    public Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, Onboarding onboarding) {
        return onboardingCompletionActivity(ctx, onboarding);
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
