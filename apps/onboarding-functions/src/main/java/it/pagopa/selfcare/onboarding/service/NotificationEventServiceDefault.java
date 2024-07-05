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
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.onboarding.utils.NotificationBuilder;
import it.pagopa.selfcare.onboarding.utils.NotificationBuilderFactory;
import it.pagopa.selfcare.onboarding.utils.QueueEventExaminer;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static it.pagopa.selfcare.onboarding.utils.Utils.isNotInstitutionOnboarding;

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
    private final NotificationBuilderFactory notificationBuilderFactory;
    private final TokenRepository tokenRepository;
    private final ObjectMapper mapper;
    private final QueueEventExaminer queueEventExaminer;

    public NotificationEventServiceDefault(ProductService productService,
                                           NotificationConfig notificationConfig,
                                           NotificationBuilderFactory notificationBuilderFactory,
                                           TokenRepository tokenRepository,
                                           QueueEventExaminer queueEventExaminer) {
        this.productService = productService;
        this.notificationConfig = notificationConfig;
        this.notificationBuilderFactory = notificationBuilderFactory;
        this.tokenRepository = tokenRepository;
        this.queueEventExaminer = queueEventExaminer;
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public void send(ExecutionContext context, Onboarding onboarding, QueueEvent queueEvent) {
        if(isNotInstitutionOnboarding(onboarding)) {
            context.getLogger().info(() -> String.format("Onboarding with ID %s doesn't refer to an institution onboarding, skipping send notification", onboarding.getId()));
            return;
        }

        Product product = productService.getProduct(onboarding.getProductId());
        if (product.getConsumers() == null || product.getConsumers().isEmpty()) {
            context.getLogger().warning(() -> String.format("Node consumers is null or empty for product with ID %s", onboarding.getProductId()));
            return;
        }

        try {
            if(Objects.isNull(queueEvent)) {
                queueEvent = queueEventExaminer.determineEventType(onboarding);
            }

            Optional<Token> token = tokenRepository.findByOnboardingId(onboarding.getId());
            InstitutionResponse institution = institutionApi.retrieveInstitutionByIdUsingGET(onboarding.getInstitution().getId());

            for (String consumer : product.getConsumers()) {
                NotificationConfig.Consumer consumerConfig = notificationConfig.consumers().get(consumer.toLowerCase());
                prepareAndSendNotification(context, product, consumerConfig, onboarding, token.orElse(null), institution, queueEvent);
            }
        } catch (Exception e) {
            throw new NotificationException(String.format("Impossible to send notification for onboarding with ID %s", onboarding.getId()), e);
        }
    }

    private void prepareAndSendNotification(ExecutionContext context, Product product, NotificationConfig.Consumer consumer, Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent) throws JsonProcessingException {
        NotificationBuilder notificationBuilder = notificationBuilderFactory.create(consumer);
        if (notificationBuilder.shouldSendNotification(onboarding, institution)) {
            NotificationToSend notificationToSend = notificationBuilder.buildNotificationToSend(onboarding, token, institution, queueEvent);
            sendNotification(context, consumer.topic(), notificationToSend);
            sendTestEnvProductsNotification(context, product, consumer.topic(), notificationToSend);
        } else {
            context.getLogger().info(() -> String.format("Notification not sent for onboarding %s on topic %s", onboarding.getId(), consumer.topic()));
        }
    }

    private void sendNotification(ExecutionContext context, String topic, NotificationToSend notificationToSend) throws JsonProcessingException {
        String message = mapper.writeValueAsString(notificationToSend);
        context.getLogger().info(() -> String.format("Sending notification on topic: %s with message: %s", topic, message));
        eventHubRestClient.sendMessage(topic, message);
    }

    private void sendTestEnvProductsNotification(ExecutionContext context, Product product, String topic, NotificationToSend notificationToSend) throws JsonProcessingException {
        if (product.getTestEnvProductIds() != null) {
            for (String testEnvProductId : product.getTestEnvProductIds()) {
                context.getLogger().info(() -> String.format("Notification for onboarding with id: %s should be sent on topic: %s for envProduct : %s", notificationToSend.getOnboardingTokenId(), topic, testEnvProductId));
                notificationToSend.setId(UUID.randomUUID().toString());
                notificationToSend.setProduct(testEnvProductId);
                sendNotification(context, topic, notificationToSend);
            }
        }
    }

}
