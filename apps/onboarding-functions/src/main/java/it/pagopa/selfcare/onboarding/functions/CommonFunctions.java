package it.pagopa.selfcare.onboarding.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.microsoft.durabletask.OrchestrationRuntimeStatus;
import com.microsoft.durabletask.PurgeInstanceCriteria;
import com.microsoft.durabletask.PurgeResult;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import com.microsoft.durabletask.azurefunctions.DurableClientInput;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.config.PurgeConfig;
import it.pagopa.selfcare.onboarding.functions.utils.SaveOnboardingStatusInput;
import it.pagopa.selfcare.onboarding.service.OnboardingService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class CommonFunctions {

    public static final String FORMAT_LOGGER_ONBOARDING_STRING = "%s: %s";
    public static final String SAVE_ONBOARDING_STATUS_ACTIVITY = "SaveOnboardingStatus";

    private final OnboardingService service;

    private final PurgeConfig purgeConfig;

    public CommonFunctions(OnboardingService service, PurgeConfig purgeConfig) {
        this.service = service;
        this.purgeConfig = purgeConfig;
    }

    @FunctionName(SAVE_ONBOARDING_STATUS_ACTIVITY)
    public void savePendingState(@DurableActivityTrigger(name = "onboardingString") String saveOnboardingStatusInputString, final ExecutionContext context) {
        SaveOnboardingStatusInput saveOnboardingStatusInput = SaveOnboardingStatusInput.readJsonString(saveOnboardingStatusInputString);
        context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SAVE_ONBOARDING_STATUS_ACTIVITY, saveOnboardingStatusInput.getOnboardingId()));
        service.savePendingState(saveOnboardingStatusInput.getOnboardingId(), OnboardingStatus.valueOf(saveOnboardingStatusInput.getStatus()));
    }

    @FunctionName("PurgeInstancesCompleted")
    public void purgeInstances(
            @TimerTrigger(name = "purgeTimer", schedule = "0 0 11 * * *") String timerInfo,
            @DurableClientInput(name = "durableContext") DurableClientContext durableContext,
            ExecutionContext context) throws TimeoutException {
        PurgeInstanceCriteria criteria = new PurgeInstanceCriteria()
                .setCreatedTimeFrom(Instant.now().minus(Duration.ofDays(purgeConfig.completedFrom())))
                .setCreatedTimeTo(Instant.now().minus(Duration.ofDays(purgeConfig.completedTo())))
                .setRuntimeStatusList(List.of(OrchestrationRuntimeStatus.COMPLETED));
        PurgeResult result = durableContext.getClient().purgeInstances(criteria);
        context.getLogger().info(String.format("Purged %d instance(s)", result.getDeletedInstanceCount()));
    }

    @FunctionName("PurgeInstancesAll")
    public void purgeInstancesAll(
            @TimerTrigger(name = "purgeTimer", schedule = "0 0 12 * * *") String timerInfo,
            @DurableClientInput(name = "durableContext") DurableClientContext durableContext,
            ExecutionContext context) throws TimeoutException {
        PurgeInstanceCriteria criteria = new PurgeInstanceCriteria()
                .setCreatedTimeFrom(Instant.now().minus(Duration.ofDays(purgeConfig.allFrom())))
                .setCreatedTimeTo(Instant.now().minus(Duration.ofDays(purgeConfig.allTo())));
        PurgeResult result = durableContext.getClient().purgeInstances(criteria);
        context.getLogger().info(String.format("Purged %d instance(s)", result.getDeletedInstanceCount()));
    }
}
