package it.pagopa.selfcare.onboarding.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.mapper.NotificationMapper;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.onboarding.utils.NotificationMapperFactory;
import it.pagopa.selfcare.onboarding.utils.SendNotificationFilter;
import it.pagopa.selfcare.onboarding.utils.SendNotificationFilterFactory;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NotificationEventServiceDefault implements NotificationEventService {

    @RestClient
    @Inject
    EventHubRestClient eventHubRestClient;

    @RestClient
    @Inject
    InstitutionApi institutionApi;

    private final ProductService productService;
    private final NotificationConfig notificationConfig;
    private final NotificationMapperFactory notificationMapperFactory;
    private final TokenRepository tokenRepository;
    private final ObjectMapper mapper;
    private final SendNotificationFilterFactory sendNotificationFilterFactory;

    public NotificationEventServiceDefault(ProductService productService,
                                           NotificationConfig notificationConfig,
                                           NotificationMapperFactory notificationMapperFactory,
                                           TokenRepository tokenRepository,
                                           SendNotificationFilterFactory sendNotificationFilterFactory) {
        this.productService = productService;
        this.notificationConfig = notificationConfig;
        this.notificationMapperFactory = notificationMapperFactory;
        this.tokenRepository = tokenRepository;
        this.sendNotificationFilterFactory = sendNotificationFilterFactory;
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public void send(ExecutionContext context, Onboarding onboarding, QueueEvent queueEvent) {
        Product product = productService.getProduct(onboarding.getProductId());
        if (product.getConsumers() == null || product.getConsumers().isEmpty()) {
            context.getLogger().warning("Node consumers is null or empty for product with ID " + onboarding.getProductId());
            return;
        }

        try {
            Optional<Token> token = tokenRepository.findByOnboardingId(onboarding.getId());
            InstitutionResponse institution = institutionApi.retrieveInstitutionByIdUsingGET(onboarding.getInstitution().getId());

            for (String consumer : product.getConsumers()) {
                String topic = notificationConfig.consumers().get(consumer.toLowerCase()).topic();
                if (shouldSendNotification(topic, onboarding, institution)) {
                    sendNotification(context, topic, onboarding, token.orElse(null), institution, queueEvent);
                    sendTestEnvProductsNotification(context, product, topic, onboarding, token.orElse(null), institution, queueEvent);
                } else {
                    context.getLogger().info(String.format("Notification not sent for onboarding %s on topic %s", onboarding.getId(), topic));
                }
            }
        } catch (Exception e) {
            context.getLogger().warning("Error during send notification for onboarding with ID " + onboarding.getId() + ". Error: " + e.getMessage());
            throw new NotificationException("Impossible to send notification for onboarding " + onboarding);
        }
    }

    private boolean shouldSendNotification(String topic, Onboarding onboarding, InstitutionResponse institution) {
        SendNotificationFilter sendNotificationFilter = sendNotificationFilterFactory.create(topic);
        return sendNotificationFilter.shouldSendNotification(onboarding, institution);
    }

    private void sendNotification(ExecutionContext context, String topic, Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent) throws JsonProcessingException {
        NotificationMapper notificationMapper = notificationMapperFactory.create(topic);
        NotificationToSend notificationToSend = notificationMapper.toNotificationToSend(onboarding, token, institution, queueEvent);
        String message = mapper.writeValueAsString(notificationToSend);
        sendMessage(context, topic, message);
    }

    private void sendTestEnvProductsNotification(ExecutionContext context, Product product, String topic, Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent) throws JsonProcessingException {
        if (product.getTestEnvProductIds() != null) {
            for (String testEnvProductId : product.getTestEnvProductIds()) {
                context.getLogger().info(String.format("Notification for onboarding with id: %s should be sent on topic: %s for envProduct : %s", onboarding.getId(), topic, testEnvProductId));
                NotificationMapper notificationMapper = notificationMapperFactory.create(topic);
                NotificationToSend notificationToSend = notificationMapper.toNotificationToSend(onboarding, token, institution, queueEvent);
                notificationToSend.setId(UUID.randomUUID().toString());
                notificationToSend.setProduct(testEnvProductId);
                String message = mapper.writeValueAsString(notificationToSend);
                sendMessage(context, topic, message);
            }
        }
    }

    private void sendMessage(ExecutionContext context, String topic, String message) {
        context.getLogger().info(String.format("Sending notification on topic: %s with message: %s", topic, message));
        eventHubRestClient.sendMessage(topic, message);
    }

}
