package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;

import java.util.Optional;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;

public record WorkflowExecutorForApprovePt(ObjectMapper objectMapper, TaskOptions optionsRetry) implements WorkflowExecutor, WorkflowExecutorTemplateInstitution {

    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingString = getOnboardingString(objectMapper, onboardingWorkflow.getOnboarding());
        ctx.callActivity(SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        return Optional.of(OnboardingStatus.TOBEVALIDATED);
    }

    @Override
    public Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, Onboarding onboarding) {
        return onboardingCompletionActivity(ctx, onboarding);
    }

    @Override
    public Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, OnboardingWorkflow onboarding) {
        return Optional.empty();
    }
}
