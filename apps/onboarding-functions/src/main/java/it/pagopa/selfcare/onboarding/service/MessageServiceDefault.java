package it.pagopa.selfcare.onboarding.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.entity.NotificationToSend;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.utils.NotificationFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MessageServiceDefault implements MessageService {

    @RestClient
    @Inject
    EventHubRestClient eventHubRestClient;
    private final Map<String, List<String>> configMap;
    private final NotificationFactory notificationFactory;
    private static final Logger log = LoggerFactory.getLogger(MessageServiceDefault.class);

    public MessageServiceDefault(@ConfigProperty(name = "onboarding-functions.event-hub.map-products-topics") String productsTopics,
                                 NotificationFactory notificationFactory) {
        try {
            this.configMap = new ObjectMapper().readValue(productsTopics, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        this.notificationFactory = notificationFactory;
    }

    @Override
    public void send(Onboarding onboarding) {
        if (!configMap.containsKey(onboarding.getProductId())) {
            log.warn("No product {} into configuration topics map", onboarding.getProductId());
            return;
        }
        try {
            for (String topic : configMap.get(onboarding.getProductId())) {
                NotificationToSend object = notificationFactory.create(topic, onboarding);
                final String message = new ObjectMapper().writeValueAsString(object);
                eventHubRestClient.sendMessage(topic, message);
                log.info("Sent notification on topic: {}", topic);
            }
        } catch (Exception e) {
            log.warn("Error during send notification for object {}: {} ", onboarding, e.getMessage(), e);
            throw new NotificationException("Impossible to send notification for object " + onboarding + ". Error: " + e.getMessage());
        }
    }
}
