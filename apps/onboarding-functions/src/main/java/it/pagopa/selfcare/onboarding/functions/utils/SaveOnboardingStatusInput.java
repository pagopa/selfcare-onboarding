package it.pagopa.selfcare.onboarding.functions.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SaveOnboardingStatusInput {

    private String status;
    private String onboardingId;

    public String getOnboardingId() {
        return onboardingId;
    }

    public String getStatus() {
        return status;
    }

    public void setOnboardingId(String onboardingId) {
        this.onboardingId = onboardingId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static SaveOnboardingStatusInput build(String onboardingId, String status) {
        SaveOnboardingStatusInput statusInput = new SaveOnboardingStatusInput();
        statusInput.setStatus(status);
        statusInput.setOnboardingId(onboardingId);
        return statusInput;
    }

    public static String buildAsJsonString(String onboardingId, String status) {
        SaveOnboardingStatusInput statusInput = new SaveOnboardingStatusInput();
        statusInput.setStatus(status);
        statusInput.setOnboardingId(onboardingId);
        return jsonString(statusInput);
    }

    public static String jsonString(SaveOnboardingStatusInput entity) {

        String jsonString;
        try {
            jsonString = new ObjectMapper().writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return jsonString;
    }


    public static SaveOnboardingStatusInput readJsonString(String jsonString) {
        try {
            return new ObjectMapper().readValue(jsonString, SaveOnboardingStatusInput.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
