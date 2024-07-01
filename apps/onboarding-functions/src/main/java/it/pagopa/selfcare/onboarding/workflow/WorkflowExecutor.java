package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.*;
public interface WorkflowExecutor {

    Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow);
    Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow);
    Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow);
    OnboardingWorkflow createOnboardingWorkflow(Onboarding onboarding);
    ObjectMapper objectMapper();
    TaskOptions optionsRetry();

    default Optional<OnboardingStatus> execute(TaskOrchestrationContext ctx, Onboarding onboarding) {
        OnboardingWorkflow onboardingWorkflow = createOnboardingWorkflow(onboarding);
        return switch (onboarding.getStatus()) {
            case REQUEST -> executeRequestState(ctx, onboardingWorkflow);
            case TOBEVALIDATED -> executeToBeValidatedState(ctx, onboardingWorkflow);
            case PENDING -> executePendingState(ctx, onboardingWorkflow);
            case REJECTED -> executeRejectedState(ctx, onboardingWorkflow);
            default -> Optional.empty();
        };

    }

    default String createInstitutionAndOnboarding(TaskOrchestrationContext ctx, Onboarding onboarding) {
        final String onboardingString = getOnboardingString(objectMapper(), onboarding);

        //CreateInstitution activity return an institutionId that is used by CreateOnboarding activity
        String institutionId = ctx.callActivity(CREATE_INSTITUTION_ACTIVITY, onboardingString, optionsRetry(), String.class).await();
        onboarding.getInstitution().setId(institutionId);
        final String onboardingWithInstitutionIdString = getOnboardingString(objectMapper(), onboarding);

        ctx.callActivity(CREATE_ONBOARDING_ACTIVITY, onboardingWithInstitutionIdString, optionsRetry(), String.class).await();
        ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingWithInstitutionIdString, optionsRetry(), String.class).await();
        ctx.callActivity(STORE_ONBOARDING_ACTIVATEDAT, onboardingWithInstitutionIdString, optionsRetry(), String.class).await();

        createTestEnvironmentsOnboarding(ctx, onboarding, onboardingWithInstitutionIdString);

        return onboardingWithInstitutionIdString;
    }

    default void createTestEnvironmentsOnboarding(TaskOrchestrationContext ctx, Onboarding onboarding, String onboardingWithInstitutionIdString) {
        // Create onboarding for test environments if exists (ex. prod-interop-coll)
        if(Objects.nonNull(onboarding.getTestEnvProductIds()) && !onboarding.getTestEnvProductIds().isEmpty()) {
            // Schedule each task to run in parallel
            List<Task<String>> parallelTasks = new ArrayList<>();
            onboarding.getTestEnvProductIds().stream()
                    .forEach(testEnvProductId -> {
                        final String onboardingStringWithTestEnvProductId = onboardingStringWithTestEnvProductId(testEnvProductId, onboardingWithInstitutionIdString);
                        parallelTasks.add(ctx.callActivity(CREATE_ONBOARDING_ACTIVITY, onboardingStringWithTestEnvProductId, optionsRetry(), String.class));
                        parallelTasks.add(ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingStringWithTestEnvProductId, optionsRetry(), String.class));
                    });

            // Wait for all tasks to complete
            ctx.allOf(parallelTasks).await();
        }
    }

    default Optional<OnboardingStatus> onboardingCompletionActivity(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        createInstitutionAndOnboarding(ctx, onboarding);
        ctx.callActivity(SEND_MAIL_COMPLETION_ACTIVITY, getOnboardingWorkflowString(objectMapper(), onboardingWorkflow), optionsRetry(), String.class).await();
        return Optional.of(OnboardingStatus.COMPLETED);
    }

    default Optional<OnboardingStatus> onboardingCompletionUsersActivity(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        final String onboardingString = getOnboardingString(objectMapper(), onboardingWorkflow.getOnboarding());
        final String onboardingWorkflowString = getOnboardingWorkflowString(objectMapper(), onboardingWorkflow);
        ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingString, optionsRetry(), String.class).await();
        ctx.callActivity(SEND_MAIL_COMPLETION_ACTIVITY, onboardingWorkflowString, optionsRetry(), String.class).await();
        return Optional.of(OnboardingStatus.COMPLETED);
    }

    default Optional<OnboardingStatus> onboardingCompletionActivityWithoutMail(TaskOrchestrationContext ctx, Onboarding onboarding) {
        createInstitutionAndOnboarding(ctx, onboarding);
        return Optional.of(OnboardingStatus.COMPLETED);
    }

    private String onboardingStringWithTestEnvProductId(String testEnvProductId, String onboardingWithInstitutionIdString) {
        Onboarding onboardingTestEnv = readOnboardingValue(objectMapper(), onboardingWithInstitutionIdString);
        onboardingTestEnv.setProductId(testEnvProductId);
        return getOnboardingString(objectMapper(), onboardingTestEnv);
    }

    default Optional<OnboardingStatus> executeRejectedState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        String onboardingString = getOnboardingString(objectMapper(), onboarding);
        if (Objects.isNull(onboarding.getReasonForReject()) ||
                (Objects.nonNull(onboarding.getReasonForReject()) && !onboarding.getReasonForReject().equals("REJECTED_BY_USER"))) {
            ctx.callActivity(SEND_MAIL_REJECTION_ACTIVITY, onboardingString, optionsRetry(), String.class).await();
        }
        return Optional.empty();
    }

}
