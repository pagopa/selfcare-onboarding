package it.pagopa.selfcare.onboarding.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.mapper.NotificationMapper;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.onboarding.utils.NotificationMapperFactory;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    private static final Logger log = LoggerFactory.getLogger(NotificationEventServiceDefault.class);
    private final ObjectMapper mapper;
    private final NotificationFactory notificationFactory;

    public NotificationEventServiceDefault(ProductService productService,
                                           NotificationConfig notificationConfig,
                                           NotificationMapperFactory notificationMapperFactory,
                                           TokenRepository tokenRepository) {
        this.productService = productService;
        this.notificationConfig = notificationConfig;
        this.notificationMapperFactory = notificationMapperFactory;
        this.tokenRepository = tokenRepository;
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void send(ExecutionContext context, Onboarding onboarding) {
    public void send(Onboarding onboarding, QueueEvent queueEvent) {
        final Product product = productService.getProduct(onboarding.getProductId());
        final Map<String, NotificationConfig.Consumer> config = notificationConfig.consumers();
        if (Objects.isNull(product.getConsumers())) {
            context.getLogger().warning("Node consumers is null for product with ID " + onboarding.getProductId());
            return;
        }

        try {
            Optional<Token> token = tokenRepository.findByOnboardingId(onboarding.getId());
            if (token.isEmpty()) {
                log.warn("Token not found for onboarding {}", onboarding.getId());
                return;
            }
            InstitutionResponse institution = institutionApi.retrieveInstitutionByIdUsingGET(onboarding.getInstitution().getId());

            for (String consumer : product.getConsumers()) {
                final String topic = config.get(consumer.toLowerCase()).topic();
                NotificationMapper notificationMapper = notificationMapperFactory.create(topic);
                final String message = mapper.writeValueAsString(notificationMapper.toNotificationToSend(onboarding, token.get(), institution, queueEvent));
                eventHubRestClient.sendMessage(topic, message);
                context.getLogger().info("Sent notification on topic: " + topic);
            }
        } catch (Exception e) {
            context.getLogger().warning("Error during send notification for object with ID " + onboarding.getId() + ". Error: " + e.getMessage());
            throw new NotificationException("Impossible to send notification for object " + onboarding);
        }
    }
}
