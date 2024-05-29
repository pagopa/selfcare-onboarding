package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FixedDelayRetry;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.service.NotificationEventService;

import java.util.Optional;

import static it.pagopa.selfcare.onboarding.functions.CommonFunctions.FORMAT_LOGGER_ONBOARDING_STRING;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.SEND_ONBOARDING_NOTIFICATION;
import static it.pagopa.selfcare.onboarding.utils.Utils.readOnboardingValue;

public class NotificationFunctions {

    private final NotificationEventService notificationEventService;
    private final ObjectMapper objectMapper;

    public NotificationFunctions(ObjectMapper objectMapper,
                                 NotificationEventService notificationEventService) {
        this.objectMapper = objectMapper;
        this.notificationEventService = notificationEventService;
    }

    /**
     * This HTTP-triggered function sends messages through event hub.
     * It gets invoked by module onboarding-cdc when status is COMPLETED or DELETED
     */
    @FunctionName("Notifications")
    @FixedDelayRetry(maxRetryCount = 3, delayInterval = "00:00:30")
    public HttpResponseMessage sendNotifications(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("sendNotifications trigger processed a request");

        // Check request body
        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Request body cannot be empty.")
                    .build();
        }

        final Onboarding onboarding;
        final String onboardingString = request.getBody().get();
        try {
            onboarding = readOnboardingValue(objectMapper, onboardingString);
            context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_ONBOARDING_NOTIFICATION, onboardingString));
        } catch (Exception ex) {
            context.getLogger().warning("Error during sendNotifications execution, msg: " + ex.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Malformed object onboarding in input.")
                    .build();
        }
        notificationEventService.send(onboarding);
        return request.createResponseBuilder(HttpStatus.OK).build();
    }
}
