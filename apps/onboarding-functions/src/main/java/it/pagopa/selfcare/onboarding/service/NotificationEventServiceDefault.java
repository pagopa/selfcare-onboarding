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
import it.pagopa.selfcare.onboarding.utils.*;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationEventServiceDefault implements NotificationEventService {

    private static final String STANDARD_CONSUMER = "standard";
    private static final Integer MAX_SIZE = 100;

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
    private final QueueEventExaminer queueEventExaminer;

    public NotificationEventServiceDefault(ProductService productService,
                                           NotificationConfig notificationConfig,
                                           NotificationBuilderFactory notificationBuilderFactory,
                                           TokenRepository tokenRepository,
                                           OnboardingRepository onboardingRepository,
                                           QueueEventExaminer queueEventExaminer) {
        this.productService = productService;
        this.notificationConfig = notificationConfig;
        this.notificationBuilderFactory = notificationBuilderFactory;
        this.tokenRepository = tokenRepository;
        this.onboardingRepository = onboardingRepository;
        this.queueEventExaminer = queueEventExaminer;
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public void send(ExecutionContext context, Onboarding onboarding, QueueEvent queueEvent) {
        Product product = productService.getProduct(onboarding.getProductId());
        if (product.getConsumers() == null || product.getConsumers().isEmpty()) {
            context.getLogger().warning(() -> String.format("Node consumers is null or empty for product with ID %s", onboarding.getProductId()));
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
            context.getLogger().warning(() -> String.format("Error during send notification for onboarding with ID %s. Error: %s", onboarding.getId(), e.getMessage()));
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

    @Override
    public FindNotificationToSendResponse findNotificationToSend(ExecutionContext context, NotificationToSendFilters filters) {
        context.getLogger().info(() -> String.format("Finding notifications to send with filters: %s", filters));
        validateFilters(filters);

        Document sort = QueryUtils.buildSortDocument("createdAt", SortEnum.DESC);
        Map<String, List<String>> queryParameter = QueryUtils.createMapForOnboardingQueryParameter(filters);
        Document query = QueryUtils.buildQuery(queryParameter);

        List<Onboarding> onboardings = onboardingRepository.find(query, sort).page(filters.getPage(), filters.getSize()).list();
        long count = onboardingRepository.find(query, sort).count();
        context.getLogger().info(() -> String.format("Found %s onboardings to send with filters", onboardings.size()));

        Map<String, InstitutionResponse> institutions = getInstitutions(onboardings);

        List<NotificationToSend> notifications = onboardings.stream()
                .map(onboarding -> buildNotificationToSend(onboarding, institutions))
                .toList();

        return new FindNotificationToSendResponse(notifications, count);
    }

    private static void validateFilters(NotificationToSendFilters filters) {
        if (filters.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException(String.format("Size cannot be greater than %s.", MAX_SIZE));
        }

        if (filters.getStatus() != null) {
            OnboardingStatus statusEnum = OnboardingStatus.valueOf(filters.getStatus());
            var allowedStates = List.of(OnboardingStatus.COMPLETED, OnboardingStatus.DELETED);
            if (!allowedStates.contains(statusEnum)) {
                throw new IllegalArgumentException("For the status field only COMPLETED and DELETED values are valid.");
            }
        }
    }

    private Map<String, InstitutionResponse> getInstitutions(List<Onboarding> onboardings) {
        Set<String> institutionsId = onboardings.stream()
                .map(onboarding -> onboarding.getInstitution().getId())
                .collect(Collectors.toSet());

        return institutionsId.stream()
                .map(id -> institutionApi.retrieveInstitutionByIdUsingGET(id))
                .collect(Collectors.toMap(InstitutionResponse::getId, Function.identity()));
    }

    private NotificationToSend buildNotificationToSend(Onboarding onboarding, Map<String, InstitutionResponse> institutions) {
        NotificationBuilder notificationBuilder = notificationBuilderFactory.create(notificationConfig.consumers().get(STANDARD_CONSUMER));
        Token token = tokenRepository.findByOnboardingId(onboarding.getId()).orElse(null);

        QueueEvent queueEvent = queueEventExaminer.determineEventType(onboarding);
        return notificationBuilder.buildNotificationToSend(onboarding, token, institutions.get(onboarding.getInstitution().getId()), queueEvent);
    }



}
