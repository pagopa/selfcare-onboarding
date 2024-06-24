package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.service.CheckOrganizationService;
import it.pagopa.selfcare.onboarding.service.NotificationEventService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;

import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.onboarding.functions.CommonFunctions.FORMAT_LOGGER_ONBOARDING_STRING;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.SEND_ONBOARDING_NOTIFICATION;
import static it.pagopa.selfcare.onboarding.utils.Utils.readOnboardingValue;

public class NotificationFunctions {

    private final NotificationEventService notificationEventService;
    private final OnboardingService onboardingService;
    private final CheckOrganizationService checkOrganizationService;
    private final ObjectMapper objectMapper;

    public NotificationFunctions(ObjectMapper objectMapper,
                                 NotificationEventService notificationEventService,
                                 OnboardingService onboardingService, CheckOrganizationService checkOrganizationService) {
        this.objectMapper = objectMapper;
        this.notificationEventService = notificationEventService;
        this.onboardingService = onboardingService;
        this.checkOrganizationService = checkOrganizationService;
    }

    /**
     * This HTTP-triggered function sends messages through event hub.
     * It gets invoked by module onboarding-cdc when status is COMPLETED or DELETED
     */
    @FunctionName("Notification")
    @FixedDelayRetry(maxRetryCount = 3, delayInterval = "00:00:30")
    public HttpResponseMessage sendNotification (
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("sendNotifications trigger processed a request");

        String onboardingString = request.getBody().orElseThrow(() -> new IllegalArgumentException("Request body cannot be empty."));
        final String queueEventString = request.getQueryParameters().get("queueEvent");
        final QueueEvent queueEvent = Objects.isNull(queueEventString) ? null : QueueEvent.valueOf(queueEventString);
        final Onboarding onboarding;

        try {
            onboarding = readOnboardingValue(objectMapper, onboardingString);
            context.getLogger().info(String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_ONBOARDING_NOTIFICATION, onboardingString));
        } catch (Exception ex) {
            context.getLogger().warning(() -> "Error during sendNotifications execution, msg: " + ex.getMessage());
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

    @FunctionName("CheckOrganization")
    public HttpResponseMessage checkOrganization(
            @HttpTrigger(name = "req", methods = {HttpMethod.HEAD}, route = "organizations", authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("checkOrganization trigger processed a request");

        String fiscalCode = request.getQueryParameters().get("fiscalCode");
        String vatNumber = request.getQueryParameters().get("vatNumber");

        if (fiscalCode == null || vatNumber == null) {
            context.getLogger().warning("fiscalCode, vatNumber are required.");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("fiscalCode, vatNumber are required.")
                    .build();
        }

        boolean alreadyRegistered =  checkOrganizationService.checkOrganization(context, fiscalCode, vatNumber);

        if (alreadyRegistered) {
            return request.createResponseBuilder(HttpStatus.OK).build();
        } else {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND).build();
        }
    }
}
