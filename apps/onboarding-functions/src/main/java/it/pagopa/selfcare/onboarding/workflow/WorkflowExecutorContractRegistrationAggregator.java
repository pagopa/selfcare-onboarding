package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowAggregator;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;

import java.util.Optional;

import static it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowType.AGGREGATOR;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingWorkflowString;

public record WorkflowExecutorContractRegistrationAggregator(ObjectMapper objectMapper, TaskOptions optionsRetry, OnboardingMapper onboardingMapper) implements WorkflowExecutor {

    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingString = getOnboardingString(objectMapper, onboardingWorkflow.getOnboarding());
        String onboardingWorkflowString = getOnboardingWorkflowString(objectMapper, onboardingWorkflow);
        ctx.callActivity(BUILD_CONTRACT_ACTIVITY_NAME, getOnboardingWorkflowString(objectMapper, onboardingWorkflow), optionsRetry, String.class).await();
        ctx.callActivity(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingWorkflowString, optionsRetry, String.class).await();
        ctx.callActivity(SEND_MAIL_REGISTRATION_FOR_CONTRACT, onboardingString, optionsRetry, String.class).await();
        return Optional.of(OnboardingStatus.PENDING);
    }

    @Override
    public Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        return Optional.empty();
    }

    @Override
    public Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        return onboardingCompletionActivity(ctx, onboardingWorkflow);
    }

    @Override
    public OnboardingWorkflow createOnboardingWorkflow(Onboarding onboarding) {
        return new OnboardingWorkflowAggregator(onboarding, AGGREGATOR.name());
    }


}
