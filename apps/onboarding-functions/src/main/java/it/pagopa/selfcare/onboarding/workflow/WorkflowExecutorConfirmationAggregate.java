package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;

import java.util.Optional;

public record WorkflowExecutorConfirmationAggregate(ObjectMapper objectMapper, TaskOptions optionsRetry) implements WorkflowExecutor{
    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, Onboarding onboarding) {
        return Optional.empty();
    }

    @Override
    public Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, Onboarding onboarding) {
        return Optional.empty();
    }

    @Override
    public Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, Onboarding onboarding) {
        return onboardingCompletionActivity(ctx, onboarding);

    }
}
