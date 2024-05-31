package it.pagopa.selfcare.onboarding.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
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

    public NotificationEventServiceDefault(ProductService productService,
                                           NotificationConfig notificationConfig,
                                           NotificationFactory notificationFactory) {
       this.productService = productService;
       this.notificationConfig = notificationConfig;
       this.notificationFactory = notificationFactory;
    }

    @Override
    public void send(ExecutionContext context, Onboarding onboarding) {
        final Product product = productService.getProduct(onboarding.getProductId());
        final Map<String, NotificationConfig.Consumer> config = notificationConfig.consumers();
        if (Objects.isNull(product.getConsumers())) {
            context.getLogger().warning("Node consumers is null for product with ID " + onboarding.getProductId());
            return;
        }
        try {
            for (String consumer : product.getConsumers()) {
                final String topic = config.get(consumer.toLowerCase()).topic();
                NotificationToSend notificationToSend = notificationFactory.create(topic, onboarding);
                final String message = new ObjectMapper().writeValueAsString(notificationToSend);
                eventHubRestClient.sendMessage(topic, message);
                context.getLogger().info("Sent notification on topic: " + topic);
            }
        } catch (Exception e) {
            context.getLogger().warning("Error during send notification for object with ID " + onboarding.getId() + ". Error: " + e.getMessage());
            throw new NotificationException("Impossible to send notification for object " + onboarding);
        }
    }
}
