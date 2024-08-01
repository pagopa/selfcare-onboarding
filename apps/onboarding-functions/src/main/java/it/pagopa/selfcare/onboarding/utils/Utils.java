package it.pagopa.selfcare.onboarding.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.HttpRequestMessage;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.dto.AckPayloadRequest;
import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.exception.FunctionOrchestratedException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;

public class Utils {

    private Utils() { }

    public static class TelemetryConstants {
        private TelemetryConstants() { }
        public static final String EVENT_SEND_REGISTRATION_FN_FAILURE = "EventsSendRegistrationEmail_failures";
        public static final String EVENT_SEND_REGISTRATION_FN_SUCCESS = "EventsSendRegistrationEmail_success";
        public static final String EVENT_SEND_REGISTRATION_CONTRACT_FN_FAILURE = "EventsSendRegistrationContractEmail_failures";
        public static final String EVENT_SEND_REGISTRATION_CONTRACT_FN_SUCCESS = "EventsSendRegistrationContractEmail_success";
        public static final String EVENT_SEND_ONBOARDING_APPROVE_FN_FAILURE = "EventsSendOnboardingApproveEmail_failures";
        public static final String EVENT_SEND_ONBOARDING_APPROVE_FN_SUCCESS = "EventsSendOnboardingApproveEmail_success";
        public static final String EVENT_SEND_REGISTRATION_APPROVE_FN_FAILURE = "EventsSendRegistrationApproveEmail_failures";
        public static final String EVENT_SEND_REGISTRATION_APPROVE_FN_SUCCESS = "EventsSendRegistrationApproveEmail_success";
        public static final String EVENT_SEND_REGISTRATION_AGGREGATOR_FN_FAILURE = "EventsSendRegistrationAggregatorEmail_failures";
        public static final String EVENT_SEND_REGISTRATION_AGGREGATOR_FN_SUCCESS = "EventsSendRegistrationAggregatorEmail_success";
        public static final String EVENT_SEND_REGISTRATION_CONTRACT_APPROVE_FN_FAILURE = "EventsSendRegistrationContractApproveEmail_failures";
        public static final String EVENT_SEND_REGISTRATION_CONTRACT_APPROVE_FN_SUCCESS = "EventsSendRegistrationContractApproveEmail_success";
        public static final String EVENT_SEND_COMPLETION_FN_FAILURE = "EventsSendCompletionEmail_failures";
        public static final String EVENT_SEND_COMPLETION_FN_SUCCESS = "EventsSendCompletionEmail_success";
        public static final String EVENT_SEND_REJECTION_FN_FAILURE = "EventsSendRejectionEmail_failures";
        public static final String EVENT_SEND_REJECTION_FN_SUCCESS = "EventsSendRejectionEmail_success";
        public static final String EVENT_SEND_COMPLETION_AGGREGATE_FN_SUCCESS = "EventsSendCompletionAggregateEmail_success";
        public static final String EVENT_SEND_COMPLETION_AGGREGATE_FN_FAILURE = "EventsSendCompletionAggregateEmail_failures";
    }

    public static final BinaryOperator<String> CONTRACT_FILENAME_FUNC =
            (filename, productName) -> String.format(filename, StringUtils.stripAccents(productName.replaceAll("\\s+","_")));

    public static final List<WorkflowType> ALLOWED_WORKFLOWS_FOR_INSTITUTION_NOTIFICATIONS = List.of(
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
        return Objects.nonNull(onboarding.getWorkflowType()) && !ALLOWED_WORKFLOWS_FOR_INSTITUTION_NOTIFICATIONS.contains(onboarding.getWorkflowType());
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

    public static ResendNotificationsFilters getResendNotificationsFilters(HttpRequestMessage<Optional<String>> request) {
        String from = request.getQueryParameters().get("from");
        String to = request.getQueryParameters().get("to");
        String productId = request.getQueryParameters().get("productId");
        List<String> status = request.getQueryParameters().get("status") == null
                ? List.of(OnboardingStatus.COMPLETED.name(), OnboardingStatus.DELETED.name())
                : List.of(request.getQueryParameters().get("status"));
        String institutionId = request.getQueryParameters().get("institutionId");
        String onboardingId = request.getQueryParameters().get("onboardingId");
        String taxCode = request.getQueryParameters().get("taxCode");

        return ResendNotificationsFilters.builder()
                .from(from)
                .to(to)
                .productId(productId)
                .institutionId(institutionId)
                .onboardingId(onboardingId)
                .taxCode(taxCode)
                .status(status)
                .build();
    }

    public static void checkResendNotificationsFilters(ResendNotificationsFilters filters) {
        boolean allowedStatus = filters.getStatus().stream()
                .allMatch(status -> status.equals(OnboardingStatus.COMPLETED.name()) || status.equals(OnboardingStatus.DELETED.name()));
        if (!allowedStatus) {
            throw new IllegalArgumentException("Status not allowed (accepted values are: " + OnboardingStatus.COMPLETED.name() + ", " + OnboardingStatus.DELETED.name() + ")");
        }

        if (StringUtils.isNotBlank(filters.getFrom()) && checkIsoDateFormat(filters.getFrom())) {
            throw new IllegalArgumentException("field from has an invalid date format (accepted format is: yyyy-MM-dd)");
        }

        if (StringUtils.isNotBlank(filters.getTo()) && checkIsoDateFormat(filters.getTo())) {
            throw new IllegalArgumentException("field to has an invalid date format (accepted format is: yyyy-MM-dd)");
        }
    }

    private static boolean checkIsoDateFormat(String date) {
        return !date.matches("\\d{4}-\\d{2}-\\d{2}");
    }
}
