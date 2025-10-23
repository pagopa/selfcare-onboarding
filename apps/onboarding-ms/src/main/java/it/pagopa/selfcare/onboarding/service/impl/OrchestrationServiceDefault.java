package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.service.OrchestrationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.onboarding_functions_json.api.OrchestrationApi;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;

@ApplicationScoped
@Data
@Slf4j
public class OrchestrationServiceDefault implements OrchestrationService {

    public static final String STARTING_ONBOARDING_ORCHESTRATION_FOR = "Starting Onboarding Orchestration for";

    @Inject
    @RestClient
    OrchestrationApi orchestrationApi;

    /**
     * Starts the onboarding orchestration for the given identifier, returning a lazy asynchronous action that emits at most one result or a failure.
     * The action is executed only upon subscription to the returned Uni.
     *
     * @param currentOnboardingId the onboarding identifier for which to start the orchestration.
     * @param timeout
     * @return a Uni that emits a single OrchestrationResponse on success or a failure on error.
     */
    @Override
    public Uni<OrchestrationResponse> triggerOrchestration(String currentOnboardingId, String timeout) {
        log.info(STARTING_ONBOARDING_ORCHESTRATION_FOR + "current onboardingId {}", currentOnboardingId);
        return orchestrationApi.apiStartOnboardingOrchestrationGet(
                currentOnboardingId, timeout);
    }

    /**
     * Triggers deletion of the institution and its user for the given onboarding identifier, returning a lazy asynchronous action that emits at most one result or a failure.
     * The action is executed only when the returned Uni is subscribed to.
     *
     * @param currentOnboardingId the onboarding identifier for which to trigger deletion of the institution and associated user.
     * @return a Uni that emits a single OrchestrationResponse on success or a failure on error.
     */
    @Override
    public Uni<OrchestrationResponse> triggerOrchestrationDeleteInstitutionAndUser(String currentOnboardingId) {
        log.info(STARTING_ONBOARDING_ORCHESTRATION_FOR + " delete Institution and User {}", currentOnboardingId);
        return orchestrationApi
                .apiTriggerDeleteInstitutionAndUserGet(currentOnboardingId);
    }

}
