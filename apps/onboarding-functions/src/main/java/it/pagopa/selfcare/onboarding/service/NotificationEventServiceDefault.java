package it.pagopa.selfcare.onboarding.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.entity.NotificationToSend;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.utils.NotificationFactory;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class NotificationEventServiceDefault implements NotificationEventService {

    @RestClient
    @Inject
    EventHubRestClient eventHubRestClient;

    private final ProductService productService;
    private final NotificationConfig notificationConfig;
    private final NotificationFactory notificationFactory;
    private static final Logger log = LoggerFactory.getLogger(NotificationEventServiceDefault.class);

    public NotificationEventServiceDefault(ProductService productService,
                                           NotificationConfig notificationConfig,
                                           NotificationFactory notificationFactory) {
       this.productService = productService;
       this.notificationConfig = notificationConfig;
       this.notificationFactory = notificationFactory;
    }

    @Override
    public void send(Onboarding onboarding) {
        final Product product = productService.getProduct(onboarding.getProductId());
        final Map<String, NotificationConfig.Consumer> config = notificationConfig.consumers();
        if (Objects.isNull(product.getConsumers())) {
            log.warn("Node consumers is null for product with ID {}", onboarding.getProductId());
            return;
        }
        try {
            for (String consumer : product.getConsumers()) {
                final String topic = config.get(consumer.toLowerCase()).topic();
                NotificationToSend notificationToSend = notificationFactory.create(topic, onboarding);
                final String message = new ObjectMapper().writeValueAsString(notificationToSend);
                eventHubRestClient.sendMessage(topic, message);
                log.info("Sent notification on topic: {}", topic);
            }
        } catch (Exception e) {
            log.warn("Error during send notification for object {}: {} ", onboarding, e.getMessage(), e);
            throw new NotificationException("Impossible to send notification for object " + onboarding);
        }
    }
}
