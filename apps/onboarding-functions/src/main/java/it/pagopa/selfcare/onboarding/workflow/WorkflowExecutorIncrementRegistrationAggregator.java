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

public record WorkflowExecutorIncrementRegistrationAggregator(ObjectMapper objectMapper, TaskOptions optionsRetry,
                                                              OnboardingMapper onboardingMapper) implements WorkflowExecutor {

    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        return Optional.empty();
    }

    @Override
    public Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        return Optional.empty();
    }

    @Override
    public Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {

        List<Task<String>> parallelTasks = new ArrayList<>();

        for (AggregateInstitution aggregate : onboardingWorkflow.getOnboarding().getAggregates()) {
            OnboardingAggregateOrchestratorInput onboardingAggregate = onboardingMapper.mapToOnboardingAggregateOrchestratorInput(onboardingWorkflow.getOnboarding(), aggregate);
            final String onboardingAggregateString = getOnboardingAggregateString(objectMapper(), onboardingAggregate);
            boolean existsDelegation = Boolean.parseBoolean(ctx.callActivity(EXISTS_DELEGATION_ACTIVITY, onboardingAggregateString, optionsRetry, String.class).await());
            if(!existsDelegation){
                parallelTasks.add(ctx.callSubOrchestrator(ONBOARDINGS_AGGREGATE_ORCHESTRATOR, onboardingAggregateString, String.class));
            }
        }

        ctx.allOf(parallelTasks).await();
        return Optional.of(OnboardingStatus.COMPLETED);
    }

    @Override
    public OnboardingWorkflow createOnboardingWorkflow(Onboarding onboarding) {
        return new OnboardingWorkflowAggregator(onboarding, AGGREGATOR.name());
    }


}
