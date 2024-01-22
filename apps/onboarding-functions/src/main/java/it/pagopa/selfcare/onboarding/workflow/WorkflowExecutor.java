package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.functions.utils.SaveOnboardingStatusInput;

import static it.pagopa.selfcare.onboarding.functions.CommonFunctions.SAVE_ONBOARDING_STATUS_ACTIVITY;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;

public interface WorkflowExecutor {

    void executeRequestState(TaskOrchestrationContext ctx, Onboarding onboarding);
    void executeToBeValidatedState(TaskOrchestrationContext ctx, Onboarding onboarding);

    ObjectMapper objectMapper();

    TaskOptions optionsRetry();

    default void execute(TaskOrchestrationContext ctx, Onboarding onboarding){
        switch (onboarding.getStatus()){
            case REQUEST -> executeRequestState(ctx, onboarding);
            case TO_BE_VALIDATED -> executeToBeValidatedState(ctx, onboarding);
            case PENDING -> executePendingState(ctx, onboarding);
        }
    }

    default void executePendingState(TaskOrchestrationContext ctx, Onboarding onboarding) {
        String onboardingString = getOnboardingString(objectMapper(), onboarding);

        //CreateInstitution activity return an institutionId that is used by CreateOnboarding activity
        String institutionId = ctx.callActivity(CREATE_INSTITUTION_ACTIVITY, onboardingString, optionsRetry(), String.class).await();
        onboarding.getInstitution().setId(institutionId);
        onboardingString = getOnboardingString(objectMapper(), onboarding);

        ctx.callActivity(CREATE_ONBOARDING_ACTIVITY, onboardingString, optionsRetry(), String.class).await();
        ctx.callActivity(SEND_MAIL_COMPLETION_ACTIVITY, onboardingString, optionsRetry(), String.class).await();

        //Last activity consist of saving pending status
        String saveOnboardingStatusInput =  SaveOnboardingStatusInput.buildAsJsonString(onboarding.getOnboardingId(), OnboardingStatus.COMPLETED.name());
        ctx.callActivity(SAVE_ONBOARDING_STATUS_ACTIVITY, saveOnboardingStatusInput, optionsRetry(), String.class).await();
    }
}
