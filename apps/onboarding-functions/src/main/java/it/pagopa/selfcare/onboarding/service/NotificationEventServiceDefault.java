package it.pagopa.selfcare.onboarding.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
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

    public NotificationEventServiceDefault(ProductService productService,
                                           NotificationConfig notificationConfig,
                                           NotificationMapperFactory notificationMapperFactory,
                                           TokenRepository tokenRepository) {
       this.productService = productService;
       this.notificationConfig = notificationConfig;
       this.notificationMapperFactory = notificationMapperFactory;
        this.tokenRepository = tokenRepository;
    }

    @Override
    public void send(Onboarding onboarding, QueueEvent queueEvent) {
        final Product product = productService.getProduct(onboarding.getProductId());
        final Map<String, NotificationConfig.Consumer> config = notificationConfig.consumers();
        if (Objects.isNull(product.getConsumers())) {
            log.warn("Node consumers is null for product with ID {}", onboarding.getProductId());
            return;
        }
        try {
            for (String consumer : product.getConsumers()) {
                final String topic = config.get(consumer.toLowerCase()).topic();
                Optional<Token> token = tokenRepository.findByOnboardingId(onboarding.getId());
                if (token.isEmpty()) {
                    log.warn("Token not found for onboarding {}", onboarding.getId());
                    return;
                }
                InstitutionResponse institution = institutionApi.retrieveInstitutionByIdUsingGET(onboarding.getInstitution().getId());
                NotificationMapper notificationMapper = notificationMapperFactory.create(topic);
                final String message = new ObjectMapper().writeValueAsString(notificationMapper.toNotificationToSend(onboarding, token.get(), institution, queueEvent));
                eventHubRestClient.sendMessage(topic, message);
                log.info("Sent notification on topic: {}", topic);
            }
        } catch (Exception e) {
            log.warn("Error during send notification for object {}: {} ", onboarding, e.getMessage(), e);
            throw new NotificationException("Impossible to send notification for object " + onboarding);
        }
    }
}
