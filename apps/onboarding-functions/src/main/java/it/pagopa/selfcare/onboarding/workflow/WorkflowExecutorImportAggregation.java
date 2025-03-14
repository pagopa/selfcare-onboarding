package it.pagopa.selfcare.onboarding.workflow;

import static it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowType.INSTITUTION;
import static it.pagopa.selfcare.onboarding.utils.Utils.readOnboardingValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowInstitution;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import java.util.Optional;

public record WorkflowExecutorImportAggregation(ObjectMapper objectMapper, TaskOptions optionsRetry, OnboardingMapper onboardingMapper) implements WorkflowExecutor {

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
        String onboardingWithInstitutionIdString = createInstitutionAndOnboarding(ctx, onboardingWorkflow.getOnboarding());
        Onboarding onboarding = readOnboardingValue(objectMapper(), onboardingWithInstitutionIdString);

        createInstitutionAndOnboardingAggregate(ctx, onboarding, onboardingMapper());

        return Optional.of(OnboardingStatus.COMPLETED);
    }

    @Override
    public OnboardingWorkflow createOnboardingWorkflow(Onboarding onboarding) {
        return new OnboardingWorkflowInstitution(onboarding, INSTITUTION.name());
    }

}
