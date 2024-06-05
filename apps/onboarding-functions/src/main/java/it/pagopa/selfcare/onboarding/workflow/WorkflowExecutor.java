package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;
import static it.pagopa.selfcare.onboarding.utils.Utils.readOnboardingValue;

public interface WorkflowExecutor {

    Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, Onboarding onboarding);
    Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, Onboarding onboarding);
    Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, Onboarding onboarding);

    ObjectMapper objectMapper();

    TaskOptions optionsRetry();

    default Optional<OnboardingStatus> execute(TaskOrchestrationContext ctx, Onboarding onboarding){
        return switch (onboarding.getStatus()) {
            case REQUEST -> executeRequestState(ctx, onboarding);
            case TOBEVALIDATED -> executeToBeValidatedState(ctx, onboarding);
            case PENDING -> executePendingState(ctx, onboarding);
            case REJECTED -> executeRejectedState(ctx, onboarding);
            default -> Optional.empty();
        };

    }

    default String createInstitutionAndOnboarding(TaskOrchestrationContext ctx, Onboarding onboarding){
        final String onboardingString = getOnboardingString(objectMapper(), onboarding);

        //CreateInstitution activity return an institutionId that is used by CreateOnboarding activity
        String institutionId = ctx.callActivity(CREATE_INSTITUTION_ACTIVITY, onboardingString, optionsRetry(), String.class).await();
        onboarding.getInstitution().setId(institutionId);
        final String onboardingWithInstitutionIdString = getOnboardingString(objectMapper(), onboarding);

        ctx.callActivity(CREATE_ONBOARDING_ACTIVITY, onboardingWithInstitutionIdString, optionsRetry(), String.class).await();
        ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingWithInstitutionIdString, optionsRetry(), String.class).await();

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

        return onboardingWithInstitutionIdString;
    }

    default Optional<OnboardingStatus> onboardingCompletionActivity(TaskOrchestrationContext ctx, Onboarding onboarding) {
        String onboardingWithInstitutionIdString = createInstitutionAndOnboarding(ctx, onboarding);
        ctx.callActivity(SEND_MAIL_COMPLETION_ACTIVITY, onboardingWithInstitutionIdString, optionsRetry(), String.class).await();
        return Optional.of(OnboardingStatus.COMPLETED);
    }

    default Optional<OnboardingStatus> onboardingCompletionAdminActivity(TaskOrchestrationContext ctx, Onboarding onboarding) {
        final String onboardingString = getOnboardingString(objectMapper(), onboarding);
        ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingString, optionsRetry(), String.class).await();
        ctx.callActivity(SEND_MAIL_COMPLETION_ACTIVITY, onboardingString, optionsRetry(), String.class).await();
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

    default Optional<OnboardingStatus> executeRejectedState(TaskOrchestrationContext ctx, Onboarding onboarding){
        String onboardingString = getOnboardingString(objectMapper(), onboarding);
        ctx.callActivity(SEND_MAIL_REJECTION_ACTIVITY, onboardingString, optionsRetry(), String.class).await();
        return Optional.empty();
    }
}
