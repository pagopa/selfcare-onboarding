package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowInstitution;

import java.util.Optional;

import static it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowType.AGGREGATOR;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;

public record WorkflowExecutorConfirmAggregate(ObjectMapper objectMapper, TaskOptions optionsRetry) implements WorkflowExecutor {

    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        return Optional.empty();
    }

    @Override
    public Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        return Optional.empty();
    }

    @Override
    public Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, Onboarding onboarding) {
        String onboardingString = getOnboardingString(objectMapper, onboarding);
        String institutionId = ctx.callActivity(CREATE_INSTITUTION_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        onboarding.getInstitution().setId(institutionId);

        final String onboardingWithInstitutionIdString = getOnboardingString(objectMapper(), onboarding);
        ctx.callActivity(CREATE_ONBOARDING_ACTIVITY, onboardingWithInstitutionIdString, optionsRetry, String.class).await();
        String delegationId = ctx.callActivity(CREATE_DELEGATION_ACTIVITY, onboardingWithInstitutionIdString, optionsRetry, String.class).await();
        onboarding.setDelegationId(delegationId);

        final String onboardingWithDelegationIdString = getOnboardingString(objectMapper(), onboarding);
        ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingWithDelegationIdString, optionsRetry, String.class).await();
        ctx.callActivity(SEND_MAIL_COMPLETION_AGGREGATE_ACTIVITY, onboardingWithDelegationIdString, optionsRetry, String.class).await();
    public Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingString = getOnboardingString(objectMapper, onboardingWorkflow.getOnboarding());
        ctx.callActivity(CREATE_INSTITUTION_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(CREATE_ONBOARDING_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(CREATE_DELEGATION_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(SEND_MAIL_COMPLETION_AGGREGATE_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        return Optional.of(OnboardingStatus.COMPLETED);
    }

    @Override
    public OnboardingWorkflow createOnboardingWorkflow(Onboarding onboarding) {
        return new OnboardingWorkflowInstitution(onboarding, AGGREGATOR.name());
    }
}
