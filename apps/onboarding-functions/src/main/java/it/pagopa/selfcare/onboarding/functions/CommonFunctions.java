package it.pagopa.selfcare.onboarding.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.functions.utils.SaveOnboardingStatusInput;
import it.pagopa.selfcare.onboarding.service.OnboardingService;

public class CommonFunctions {

    public static final String FORMAT_LOGGER_ONBOARDING_STRING = "%s: %s";
    public static final String SAVE_ONBOARDING_STATUS_ACTIVITY = "SaveOnboardingStatus";

    private final OnboardingService service;

    public CommonFunctions(OnboardingService service) {
        this.service = service;
    }

    @FunctionName(SAVE_ONBOARDING_STATUS_ACTIVITY)
    public void savePendingState(@DurableActivityTrigger(name = "onboardingString") String saveOnboardingStatusInputString, final ExecutionContext context) {
        SaveOnboardingStatusInput saveOnboardingStatusInput = SaveOnboardingStatusInput.readJsonString(saveOnboardingStatusInputString);
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SAVE_ONBOARDING_STATUS_ACTIVITY, saveOnboardingStatusInput.getOnboardingId()));
        service.savePendingState(saveOnboardingStatusInput.getOnboardingId(), OnboardingStatus.valueOf(saveOnboardingStatusInput.getStatus()));
    }
}
