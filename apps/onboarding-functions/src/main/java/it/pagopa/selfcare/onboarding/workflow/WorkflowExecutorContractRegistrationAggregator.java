package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.AggregateInstitution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowAggregator;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowType.AGGREGATOR;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.*;

public record WorkflowExecutorContractRegistrationAggregator(ObjectMapper objectMapper, TaskOptions optionsRetry) implements WorkflowExecutor {

    public static OnboardingMapper onboardingMapper;

    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingString = getOnboardingString(objectMapper, onboardingWorkflow.getOnboarding());
        ctx.callActivity(BUILD_CONTRACT_ACTIVITY_NAME, getOnboardingWorkflowString(objectMapper, onboardingWorkflow), optionsRetry, String.class).await();
        ctx.callActivity(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(SEND_MAIL_REGISTRATION_FOR_CONTRACT, onboardingString, optionsRetry, String.class).await();
        return Optional.of(OnboardingStatus.PENDING);
    }

    @Override
    public Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        return Optional.empty();
    }

    @Override
    public Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingWithInstitutionIdString = createInstitutionAndOnboarding(ctx, onboardingWorkflow.getOnboarding());
        Onboarding onboarding = readOnboardingValue(objectMapper(), onboardingWithInstitutionIdString);

        List<Task<Void>> parallelTasks = new ArrayList<>();

        for (AggregateInstitution aggregate : onboarding.getAggregates()) {
            OnboardingAggregateOrchestratorInput onboardingAggregate = onboardingMapper.mapToOnboardingAggregateOrchestratorInput(onboarding, aggregate);
            final String onboardingAggregateString = getOnboardingAggregateString(objectMapper(), onboardingAggregate);
            parallelTasks.add(ctx.callSubOrchestrator(ONBOARDINGS_AGGREGATE_ORCHESTRATOR, onboardingAggregateString));
        }

        ctx.allOf(parallelTasks).await();

        ctx.callActivity(SEND_MAIL_COMPLETION_ACTIVITY, onboardingWithInstitutionIdString, optionsRetry, String.class).await();
        return Optional.of(OnboardingStatus.COMPLETED);
    }

    @Override
    public OnboardingWorkflow createOnboardingWorkflow(Onboarding onboarding) {
        return new OnboardingWorkflowAggregator(onboarding, AGGREGATOR.name());
    }


}
