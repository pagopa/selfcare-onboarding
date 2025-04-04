package it.pagopa.selfcare.onboarding.steps;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class ScenarioContext {

    @Inject
    IntegrationOnboardingPAResources paResources;

    private Map<String, String> requestBodies = new HashMap<>();
    private String currentKey;

    public void storeRequestBody(String key) {
        requestBodies.put(key, paResources.getJsonTemplate(key));
        currentKey = key;
    }

    public String getCurrentRequestBody() {
        return currentKey != null ? requestBodies.get(currentKey) : null;
    }
}