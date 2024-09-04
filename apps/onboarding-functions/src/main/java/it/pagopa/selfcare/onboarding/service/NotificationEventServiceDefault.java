package it.pagopa.selfcare.onboarding.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.NotificationsResources;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.onboarding.utils.NotificationBuilder;
import it.pagopa.selfcare.onboarding.utils.NotificationBuilderFactory;
import it.pagopa.selfcare.onboarding.utils.QueueEventExaminer;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

import java.util.*;

import static it.pagopa.selfcare.onboarding.utils.Utils.isNotInstitutionOnboarding;

@ApplicationScoped
public class NotificationEventServiceDefault implements NotificationEventService {

    public static final String EVENT_ONBOARDING_FN_NAME = "ONBOARDING-FN";
    public static final String EVENT_ONBOARDING_INSTTITUTION_FN_FAILURE = "EventsOnboardingInstitution_failures";
    public static final String EVENT_ONBOARDING_INSTTITUTION_FN_SUCCESS = "EventsOnboardingInstitution_success";
    public static final String OPERATION_NAME = "ONBOARDING-FN";
    private final TelemetryClient telemetryClient;
    @RestClient
    @Inject
    EventHubRestClient eventHubRestClient;

    @RestClient
    @Inject
    InstitutionApi institutionApi;

    private final ProductService productService;
    private final NotificationConfig notificationConfig;
    private final NotificationBuilderFactory notificationBuilderFactory;
    private final TokenRepository tokenRepository;
    private final ObjectMapper mapper;
    private final QueueEventExaminer queueEventExaminer;

    public NotificationEventServiceDefault(ProductService productService,
                                           NotificationConfig notificationConfig,
                                           NotificationBuilderFactory notificationBuilderFactory,
                                           TokenRepository tokenRepository,
                                           QueueEventExaminer queueEventExaminer,
                                           @Context @ConfigProperty(name = "onboarding-functions.appinsights.connection-string") String appInsightsConnectionString) {
        this.productService = productService;
        this.notificationConfig = notificationConfig;
        this.notificationBuilderFactory = notificationBuilderFactory;
        this.tokenRepository = tokenRepository;
        this.queueEventExaminer = queueEventExaminer;
        TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.createDefault();
        telemetryConfiguration.setConnectionString(appInsightsConnectionString);
        this.telemetryClient = new TelemetryClient(telemetryConfiguration);
        this.telemetryClient.getContext().getOperation().setName(OPERATION_NAME);
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public void send(ExecutionContext context, Onboarding onboarding, QueueEvent queueEvent) {
        this.send(context, onboarding, queueEvent, null);
    }

    @Override
    public void send(ExecutionContext context, Onboarding onboarding, QueueEvent queueEvent, String notificationEventTraceId) {
        context.getLogger().info(() -> String.format("Starting send method for onboarding with ID %s", onboarding.getId()));
        if(isNotInstitutionOnboarding(onboarding)) {
            context.getLogger().info(() -> String.format("Onboarding with ID %s doesn't refer to an institution onboarding, skipping send notification", onboarding.getId()));
            return;
        }

        context.getLogger().info(() -> String.format("Getting product info for onboarding with ID %s and productId %s", onboarding.getId(), onboarding.getProductId()));
        Product product = productService.getProduct(onboarding.getProductId());
        if (product.getConsumers() == null || product.getConsumers().isEmpty()) {
            context.getLogger().warning(() -> String.format("Node consumers is null or empty for product with ID %s", onboarding.getProductId()));
            return;
        }

        if(Objects.isNull(queueEvent)) {
            queueEvent = queueEventExaminer.determineEventType(onboarding);
        }

        context.getLogger().info(() -> String.format("Retrieving institution having ID %s", onboarding.getInstitution().getId()));
        InstitutionResponse institution = institutionApi.retrieveInstitutionByIdUsingGET(onboarding.getInstitution().getId());

        Token token = tokenRepository.findByOnboardingId(onboarding.getId()).orElse(null);
        NotificationsResources notificationsResources = new NotificationsResources(onboarding, institution, token, queueEvent);
        for (String consumer : product.getConsumers()) {
            NotificationConfig.Consumer consumerConfig = notificationConfig.consumers().get(consumer.toLowerCase());
            prepareAndSendNotification(context, product, consumerConfig, notificationsResources, notificationEventTraceId);
        }
    }

    private void prepareAndSendNotification(ExecutionContext context, Product product, NotificationConfig.Consumer consumer, NotificationsResources notificationsResources, String notificationEventTraceId) {
        NotificationBuilder notificationBuilder = notificationBuilderFactory.create(consumer);
        if (notificationBuilder.shouldSendNotification(notificationsResources.getOnboarding(), notificationsResources.getInstitution())) {
            NotificationToSend notificationToSend = notificationBuilder.buildNotificationToSend(notificationsResources.getOnboarding(), notificationsResources.getToken(), notificationsResources.getInstitution(), notificationsResources.getQueueEvent());
            sendNotification(context, consumer.topic(), notificationToSend, notificationEventTraceId);
            sendTestEnvProductsNotification(context, product, consumer.topic(), notificationToSend, notificationEventTraceId);
        } else {
            context.getLogger().info(() -> String.format("It was not necessary to send a notification on the topic %s because the onboarding with ID %s did not pass filter verification", notificationsResources.getOnboarding().getId(), consumer.topic()));
        }
    }

    private void sendNotification(ExecutionContext context, String topic, NotificationToSend notificationToSend, String notificationEventTraceId) {
        String message = null;
        try {
            message = mapper.writeValueAsString(notificationToSend);
        } catch (JsonProcessingException e) {
            throw new NotificationException("Notification cannot be serialized");
        } finally {
            String finalMessage = message;
            context.getLogger().info(() -> String.format("Sending notification on topic: %s with message: %s", topic, finalMessage));
        }

        eventHubRestClient.sendMessage(topic, message);
        telemetryClient.trackEvent(EVENT_ONBOARDING_FN_NAME, notificationEventMap(notificationToSend, topic, notificationEventTraceId),  Map.of(EVENT_ONBOARDING_INSTTITUTION_FN_SUCCESS, 1D));
    }

    private void sendTestEnvProductsNotification(ExecutionContext context, Product product, String topic, NotificationToSend notificationToSend, String notificationEventTraceId) {
        context.getLogger().info(() -> String.format("Starting sendTestEnvProductsNotification with testEnv %s", product.getTestEnvProductIds()));
        if (product.getTestEnvProductIds() != null) {
            for (String testEnvProductId : product.getTestEnvProductIds()) {
                context.getLogger().info(() -> String.format("Notification for onboarding with id: %s should be sent on topic: %s for envProduct : %s", notificationToSend.getOnboardingTokenId(), topic, testEnvProductId));
                notificationToSend.setId(UUID.randomUUID().toString());
                notificationToSend.setProduct(testEnvProductId);
                sendNotification(context, topic, notificationToSend, notificationEventTraceId);
            }
        }
    }

    public static Map<String, String> onboardingEventMap(Onboarding onboarding) {
        Map<String, String> propertiesMap = new HashMap<>();
        Optional.ofNullable(onboarding.getId()).ifPresent(value -> propertiesMap.put("id", value));
        return propertiesMap;
    }

    public static Map<String, String> onboardingEventFailureMap(Onboarding onboarding, Exception e) {
        return onboardingEventFailureMap(onboarding, e, null);
    }

    public static Map<String, String> onboardingEventFailureMap(Onboarding onboarding, Exception e, String notificationEventTraceId) {
        Map<String, String> propertiesMap = onboardingEventMap(onboarding);
        Optional.ofNullable(notificationEventTraceId).ifPresent(value -> propertiesMap.put("notificationEventTraceId", value));
        Optional.ofNullable(e).ifPresent(value -> propertiesMap.put("error", Arrays.toString(e.getStackTrace())));
        return propertiesMap;
    }

    public static Map<String, String> notificationEventMap(NotificationToSend notificationToSend, String topic, String notificationEventTraceId) {
        Map<String, String> propertiesMap = new HashMap<>();
        Optional.ofNullable(topic).ifPresent(value -> propertiesMap.put("topic", value));
        Optional.ofNullable(notificationEventTraceId).ifPresent(value -> propertiesMap.put("notificationEventTraceId", value));
        Optional.ofNullable(notificationToSend.getId()).ifPresent(value -> propertiesMap.put("id", value));
        Optional.ofNullable(notificationToSend.getInternalIstitutionID()).ifPresent(value -> propertiesMap.put("internalIstitutionID", value));
        Optional.ofNullable(notificationToSend.getInstitutionId()).ifPresent(value -> propertiesMap.put("institutionId", value));
        Optional.ofNullable(notificationToSend.getProduct()).ifPresent(value -> propertiesMap.put("product", value));
        Optional.ofNullable(notificationToSend.getState()).ifPresent(value -> propertiesMap.put("state", value));
        Optional.ofNullable(notificationToSend.getFilePath()).ifPresent(value -> propertiesMap.put("filePath", value));
        Optional.ofNullable(notificationToSend.getFileName()).ifPresent(value -> propertiesMap.put("fileName", value));
        Optional.ofNullable(notificationToSend.getContentType()).ifPresent(value -> propertiesMap.put("contentType", value));
        Optional.ofNullable(notificationToSend.getOnboardingTokenId()).ifPresent(value -> propertiesMap.put("onboardingTokenId", value));
        Optional.ofNullable(notificationToSend.getPricingPlan()).ifPresent(value -> propertiesMap.put("pricingPlan", value));

        if (Optional.ofNullable(notificationToSend.getInstitution()).isPresent()) {
            Optional.ofNullable(notificationToSend.getInstitution().getDescription()).ifPresent(value -> propertiesMap.put("description", value));
            Optional.ofNullable(notificationToSend.getInstitution().getInstitutionType()).ifPresent(value -> propertiesMap.put("institutionType", value.name()));
            Optional.ofNullable(notificationToSend.getInstitution().getDigitalAddress()).ifPresent(value -> propertiesMap.put("digitalAddress", value));
            Optional.ofNullable(notificationToSend.getInstitution().getAddress()).ifPresent(value -> propertiesMap.put("address", value));
            Optional.ofNullable(notificationToSend.getInstitution().getTaxCode()).ifPresent(value -> propertiesMap.put("taxCode", value));
            Optional.ofNullable(notificationToSend.getInstitution().getOrigin()).ifPresent(value -> propertiesMap.put("origin", value));
            Optional.ofNullable(notificationToSend.getInstitution().getOriginId()).ifPresent(value -> propertiesMap.put("originId", value));
            Optional.ofNullable(notificationToSend.getInstitution().getIstatCode()).ifPresent(value -> propertiesMap.put("istatCode", value));
            Optional.ofNullable(notificationToSend.getInstitution().getCity()).ifPresent(value -> propertiesMap.put("city", value));
            Optional.ofNullable(notificationToSend.getInstitution().getCountry()).ifPresent(value -> propertiesMap.put("country", value));
            Optional.ofNullable(notificationToSend.getInstitution().getCounty()).ifPresent(value -> propertiesMap.put("county", value));
            Optional.ofNullable(notificationToSend.getInstitution().getSubUnitCode()).ifPresent(value -> propertiesMap.put("subUnitCode", value));
            Optional.ofNullable(notificationToSend.getInstitution().getCategory()).ifPresent(value -> propertiesMap.put("category", value));
            Optional.ofNullable(notificationToSend.getInstitution().getSubUnitType()).ifPresent(value -> propertiesMap.put("subUnitType", value));
            if (Optional.ofNullable(notificationToSend.getInstitution().getRootParent()).isPresent()) {
                Optional.ofNullable(notificationToSend.getInstitution().getRootParent().getId()).ifPresent(value -> propertiesMap.put("root.parentId", value));
                Optional.ofNullable(notificationToSend.getInstitution().getRootParent().getDescription()).ifPresent(value -> propertiesMap.put("root.parentDescription", value));
                Optional.ofNullable(notificationToSend.getInstitution().getRootParent().getOriginId()).ifPresent(value -> propertiesMap.put("root.parentOriginId", value));
            }
        }

        if (Optional.ofNullable(notificationToSend.getBilling()).isPresent()) {
            Optional.ofNullable(notificationToSend.getBilling().getRecipientCode()).ifPresent(value -> propertiesMap.put("billing.recipientCode", value));
            Optional.ofNullable(notificationToSend.getBilling().getTaxCodeInvoicing()).ifPresent(value -> propertiesMap.put("billing.TaxCodeInvoicing", value));
            Optional.ofNullable(notificationToSend.getBilling().getVatNumber()).ifPresent(value -> propertiesMap.put("billing.VatNumber", value));
            Optional.ofNullable(notificationToSend.getBilling().isPublicService()).ifPresent(value -> propertiesMap.put("billing.isPublicService", Boolean.TRUE.equals(value) ? "true" : "false"));
        }

        return propertiesMap;
    }
}
