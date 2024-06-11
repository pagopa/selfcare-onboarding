package it.pagopa.selfcare.onboarding.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.FindNotificationToSendResponse;
import it.pagopa.selfcare.onboarding.dto.NotificationToSendFilters;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.utils.NotificationBuilder;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.onboarding.utils.NotificationBuilderFactory;
import it.pagopa.selfcare.onboarding.utils.QueryUtils;
import it.pagopa.selfcare.onboarding.utils.SortEnum;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

import java.util.Optional;
import java.util.UUID;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationEventServiceDefault implements NotificationEventService {

    private static final String STANDARD_CONSUMER = "standard";
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
    private final OnboardingRepository onboardingRepository;

    public NotificationEventServiceDefault(ProductService productService,
                                           NotificationConfig notificationConfig,
                                           NotificationBuilderFactory notificationBuilderFactory,
                                           TokenRepository tokenRepository,
                                           OnboardingRepository onboardingRepository) {
        this.productService = productService;
        this.notificationConfig = notificationConfig;
        this.notificationBuilderFactory = notificationBuilderFactory;
        this.tokenRepository = tokenRepository;
        this.onboardingRepository = onboardingRepository;
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public void send(ExecutionContext context, Onboarding onboarding, QueueEvent queueEvent) {
        Product product = productService.getProduct(onboarding.getProductId());
        if (product.getConsumers() == null || product.getConsumers().isEmpty()) {
            context.getLogger().warning(String.format("Node consumers is null or empty for product with ID %s", onboarding.getProductId()));
            return;
        }

        try {
            Optional<Token> token = tokenRepository.findByOnboardingId(onboarding.getId());
            InstitutionResponse institution = institutionApi.retrieveInstitutionByIdUsingGET(onboarding.getInstitution().getId());

            for (String consumer : product.getConsumers()) {
                NotificationConfig.Consumer consumerConfig = notificationConfig.consumers().get(consumer.toLowerCase());
                prepareAndSendNotification(context, product, consumerConfig, onboarding, token.orElse(null), institution, queueEvent);
            }
        } catch (Exception e) {
            context.getLogger().warning(String.format("Error during send notification for onboarding with ID %s. Error: %s", onboarding.getId(), e.getMessage()));
            throw new NotificationException(String.format("Impossible to send notification for onboarding %s", onboarding));
        }
    }


    private void prepareAndSendNotification(ExecutionContext context, Product product, NotificationConfig.Consumer consumer, Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent) throws JsonProcessingException {
        NotificationBuilder notificationBuilder = notificationBuilderFactory.create(consumer);
        if (notificationBuilder.shouldSendNotification(onboarding, institution)) {
            NotificationToSend notificationToSend = notificationBuilder.buildNotificationToSend(onboarding, token, institution, queueEvent);
            sendNotification(context, consumer.topic(), notificationToSend);
            sendTestEnvProductsNotification(context, product, consumer.topic(), notificationToSend);
        } else {
            context.getLogger().info(String.format("Notification not sent for onboarding %s on topic %s", onboarding.getId(), consumer.topic()));
        }
    }

    private void sendNotification(ExecutionContext context, String topic, NotificationToSend notificationToSend) throws JsonProcessingException {
        String message = mapper.writeValueAsString(notificationToSend);
        context.getLogger().info(String.format("Sending notification on topic: %s with message: %s", topic, message));
        eventHubRestClient.sendMessage(topic, message);
    }

    private void sendTestEnvProductsNotification(ExecutionContext context, Product product, String topic, NotificationToSend notificationToSend) throws JsonProcessingException {
        if (product.getTestEnvProductIds() != null) {
            for (String testEnvProductId : product.getTestEnvProductIds()) {
                context.getLogger().info(String.format("Notification for onboarding with id: %s should be sent on topic: %s for envProduct : %s", notificationToSend.getOnboardingTokenId(), topic, testEnvProductId));
                notificationToSend.setId(UUID.randomUUID().toString());
                notificationToSend.setProduct(testEnvProductId);
                sendNotification(context, topic, notificationToSend);
            }
        }
    }

    @Override
    public FindNotificationToSendResponse findNotificationToSend(ExecutionContext context, NotificationToSendFilters filters) {
        context.getLogger().info(String.format("Finding notifications to send with filters: %s", filters));

        if (filters.getStatus() != null) {
            OnboardingStatus statusEnum = OnboardingStatus.valueOf(filters.getStatus());
            var allowedStates = List.of(OnboardingStatus.COMPLETED, OnboardingStatus.DELETED);
            if (!allowedStates.contains(statusEnum)) {
                throw new IllegalArgumentException("For the status field only COMPLETED and DELETED values are valid.");
            }
        }

        Document sort = QueryUtils.buildSortDocument("createdAt", SortEnum.DESC);
        Map<String, List<String>> queryParameter = QueryUtils.createMapForOnboardingQueryParameter(filters);
        Document query = QueryUtils.buildQuery(queryParameter);

        List<Onboarding> onboardings = onboardingRepository.find(query, sort).page(filters.getPage(), filters.getSize()).list();
        long count = onboardingRepository.find(query, sort).count();
        context.getLogger().info(String.format("Found %s onboardings", onboardings.size()));

        Set<String> institutionsId = onboardings.stream()
                .map(onboarding -> onboarding.getInstitution().getId())
                .collect(Collectors.toSet());

        // TODO : Testare caso di eccezione
        Map<String, org.openapi.quarkus.core_json.model.InstitutionResponse> institutions = institutionsId.stream()
                .map(id -> institutionApi.retrieveInstitutionByIdUsingGET(id))
                .collect(Collectors.toMap(InstitutionResponse::getId, institutionResponse -> institutionResponse));

        NotificationConfig.Consumer consumerConfig = notificationConfig.consumers().get(STANDARD_CONSUMER);
        NotificationBuilder notificationBuilder = notificationBuilderFactory.create(consumerConfig);
        List<NotificationToSend> notificationToSends = onboardings.stream()
                .map(onboarding -> {
                    org.openapi.quarkus.core_json.model.InstitutionResponse institution = institutions.get(onboarding.getInstitution().getId());
                    Token token = tokenRepository.findByOnboardingId(onboarding.getId()).orElseThrow();
                    // TODO : è giusto considerare UPDATE?
                    return notificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.UPDATE);
                }).toList();

        return new FindNotificationToSendResponse(notificationToSends, count);
    }

}
