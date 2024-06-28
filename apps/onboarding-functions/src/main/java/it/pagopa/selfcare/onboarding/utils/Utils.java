package it.pagopa.selfcare.onboarding.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.dto.AckPayloadRequest;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.exception.FunctionOrchestratedException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.BiFunction;

public class Utils {

    public static final BiFunction<String, String, String> CONTRACT_FILENAME_FUNC =
            (filename, productName) -> String.format(filename, StringUtils.stripAccents(productName.replaceAll("\\s+","_")));

    private static final List<WorkflowType> ALLOWED_WORKFLOWS_FOR_INSTITUTION_NOTIFICATIONS = List.of(
            WorkflowType.CONFIRMATION,
            WorkflowType.FOR_APPROVE,
            WorkflowType.IMPORT,
            WorkflowType.CONTRACT_REGISTRATION,
            WorkflowType.FOR_APPROVE_PT
    );

    public static Onboarding readOnboardingValue(ObjectMapper objectMapper, String onboardingString) {
        try {
            return objectMapper.readValue(onboardingString, Onboarding.class);
        } catch (JsonProcessingException e) {
            throw new FunctionOrchestratedException(e);
        }
    }

    public static OnboardingAggregateOrchestratorInput readOnboardingAggregateOrchestratorInputValue(ObjectMapper objectMapper, String onboardingAggregateOrchestratorInputString) {
        try {
            return objectMapper.readValue(onboardingAggregateOrchestratorInputString, OnboardingAggregateOrchestratorInput.class);
        } catch (JsonProcessingException e) {
            throw new FunctionOrchestratedException(e);
        }
    }

    public static OnboardingWorkflow readOnboardingWorkflowValue(ObjectMapper objectMapper, String onboardingString) {
        try {
            return objectMapper.readValue(onboardingString, OnboardingWorkflow.class);
        } catch (JsonProcessingException e) {
            throw new FunctionOrchestratedException(e);
        }
    }

    public static String getOnboardingString(ObjectMapper objectMapper, Onboarding onboarding) {

        String onboardingString;
        try {
            onboardingString = objectMapper.writeValueAsString(onboarding);
        } catch (JsonProcessingException e) {
            throw new FunctionOrchestratedException(e);
        }
        return onboardingString;
    }

    public static String getOnboardingWorkflowString(ObjectMapper objectMapper, OnboardingWorkflow onboardingWorkflow) {

        String onboardingWorkflowString;
        try {
            onboardingWorkflowString = objectMapper.writeValueAsString(onboardingWorkflow);
        } catch (JsonProcessingException e) {
            throw new FunctionOrchestratedException(e);
        }
        return onboardingWorkflowString;
    }

    public static AckPayloadRequest readAckPayloadValue(ObjectMapper objectMapper, String ackPayloadString) {
        try {
            return objectMapper.readValue(ackPayloadString, AckPayloadRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static boolean isNotInstitutionOnboarding(Onboarding onboarding) {
        return !ALLOWED_WORKFLOWS_FOR_INSTITUTION_NOTIFICATIONS.contains(onboarding.getWorkflowType());
    }

    public static String getOnboardingAggregateString(ObjectMapper objectMapper, OnboardingAggregateOrchestratorInput onboarding) {

        String onboardingAggregateString;
        try {
            onboardingAggregateString = objectMapper.writeValueAsString(onboarding);
        } catch (JsonProcessingException e) {
            throw new FunctionOrchestratedException(e);
        }
        return onboardingAggregateString;
    }
}
