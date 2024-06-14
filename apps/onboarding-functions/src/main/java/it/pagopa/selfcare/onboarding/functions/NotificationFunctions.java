package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FixedDelayRetry;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import it.pagopa.selfcare.onboarding.dto.FindNotificationToSendResponse;
import it.pagopa.selfcare.onboarding.dto.NotificationToSendFilters;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.service.NotificationEventService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.ws.rs.core.MediaType;

import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.onboarding.functions.CommonFunctions.FORMAT_LOGGER_ONBOARDING_STRING;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.SEND_ONBOARDING_NOTIFICATION;
import static it.pagopa.selfcare.onboarding.utils.Utils.readOnboardingValue;

public class NotificationFunctions {

    private final NotificationEventService notificationEventService;
    private final OnboardingService onboardingService;
    private final ObjectMapper objectMapper;

    public NotificationFunctions(ObjectMapper objectMapper,
                                 NotificationEventService notificationEventService,
                                 OnboardingService onboardingService) {
        this.objectMapper = objectMapper;
        this.notificationEventService = notificationEventService;
        this.onboardingService = onboardingService;
    }

    /**
     * This HTTP-triggered function sends messages through event hub.
     * It gets invoked by module onboarding-cdc when status is COMPLETED or DELETED
     */
    @FunctionName("Notification")
    @FixedDelayRetry(maxRetryCount = 3, delayInterval = "00:00:30")
    public HttpResponseMessage sendNotification(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("sendNotifications trigger processed a request");

        final String queueEventString = request.getQueryParameters().get("queueEvent");
        final QueueEvent queueEvent = Objects.isNull(queueEventString) ? QueueEvent.ADD : QueueEvent.valueOf(queueEventString);

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
        notificationEventService.send(context, onboarding, queueEvent);
        return request.createResponseBuilder(HttpStatus.OK).build();
    }

    /**
     * This HTTP-triggered function retrieves onboarding given its identifier
     * After that, It sends a message on topics through the event bus
     */
    @FunctionName("ResendNotification")
    public HttpResponseMessage resendNotification(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("sendNotifications trigger processed a request");

        final String onboardingId = request.getQueryParameters().get("onboardingId");
        if (Objects.isNull(onboardingId)) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("onboardingId cannot be empty.")
                    .build();
        }

        final String queueEventString = request.getQueryParameters().get("queueEvent");
        final QueueEvent queueEvent = Objects.isNull(queueEventString) ? QueueEvent.UPDATE : QueueEvent.valueOf(queueEventString);


        final Optional<Onboarding> onboarding = onboardingService.getOnboarding(onboardingId);
        if(onboarding.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body("Onboarding with ID: " + onboardingId + " not found")
                    .build();
        }
        notificationEventService.send(context, onboarding.get(), queueEvent);
        return request.createResponseBuilder(HttpStatus.OK).build();
    }


    @FunctionName("FindNotifications")
    public HttpResponseMessage findNotificationsToSend(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) throws JsonProcessingException {
        context.getLogger().info("findNotificationsToSend trigger processed a request");

        NotificationToSendFilters filters = getNotificationToSendFilters(request);
        FindNotificationToSendResponse response;
        try {
            response = notificationEventService.findNotificationToSend(context, filters);
        } catch (IllegalArgumentException iex) {
            context.getLogger().warning("Error during findNotificationToSend execution, msg: " + iex.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(iex.getMessage())
                    .build();
        } catch (Exception ex) {
            context.getLogger().warning("Error during findNotificationToSend execution, msg: " + ex.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during findNotificationToSend execution")
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .body(objectMapper.writeValueAsString(response))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
    }

    private static NotificationToSendFilters getNotificationToSendFilters(HttpRequestMessage<Optional<String>> request) {
        final String productId = request.getQueryParameters().get("productId");
        final String institutionId = request.getQueryParameters().get("institutionId");
        final String onboardingId = request.getQueryParameters().get("onboardingId");
        final String taxCode = request.getQueryParameters().get("taxCode");
        final String from = request.getQueryParameters().get("from");
        final String to = request.getQueryParameters().get("to");
        final String status = request.getQueryParameters().get("status");
        final Integer page = Integer.parseInt(request.getQueryParameters().getOrDefault("page", "0"));
        final Integer size = Integer.parseInt(request.getQueryParameters().getOrDefault("size", "20"));

        NotificationToSendFilters filters = new NotificationToSendFilters();
        filters.setProductId(productId);
        filters.setInstitutionId(institutionId);
        filters.setOnboardingId(onboardingId);
        filters.setTaxCode(taxCode);
        filters.setStatus(status);
        filters.setFrom(from);
        filters.setTo(to);
        filters.setPage(page);
        filters.setSize(size);
        return filters;
    }
}
