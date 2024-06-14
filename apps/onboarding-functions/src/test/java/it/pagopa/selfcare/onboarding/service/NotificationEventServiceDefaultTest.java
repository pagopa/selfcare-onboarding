package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.dto.FindNotificationToSendResponse;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.NotificationToSendFilters;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.utils.*;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
public class NotificationEventServiceDefaultTest {

    @Inject
    NotificationEventServiceDefault notificationEventServiceDefault;

    @InjectMock
    ProductService productService;

    @RestClient
    @InjectMock
    EventHubRestClient eventHubRestClient;

    @InjectMock
    NotificationBuilderFactory notificationBuilderFactory;

    @InjectMock
    TokenRepository tokenRepository;

    @InjectMock
    OnboardingRepository onboardingRepository;

    @InjectMock
    QueueEventExaminer queueEventExaminer;

    @RestClient
    @InjectMock
    InstitutionApi institutionApi;


    @Test
    void sendMessage() {
        final Onboarding onboarding = createOnboarding();
        final Product product = createProduct();
        when(productService.getProduct(any())).thenReturn(product);
        mockNotificationMapper(true);
        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(new InstitutionResponse());
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        notificationEventServiceDefault.send(context, onboarding, QueueEvent.ADD);
        verify(eventHubRestClient, times(3))
                .sendMessage(anyString(), anyString());
    }

    @Test
    void sendMessageWithoutToken() {
        final Onboarding onboarding = createOnboarding();
        final Product product = createProduct();
        when(productService.getProduct(any())).thenReturn(product);
        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.empty());
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(new InstitutionResponse());
        mockNotificationMapper(true);
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        notificationEventServiceDefault.send(context, onboarding, QueueEvent.ADD);
        verify(eventHubRestClient, times(3))
                .sendMessage(anyString(), anyString());
    }

    @Test
    void sendMessageDoesntSendNotificationIfFilterDoesntAllow() {
        final Onboarding onboarding = createOnboarding();
        final Product product = createProduct();
        when(productService.getProduct(any())).thenReturn(product);
        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(new InstitutionResponse());
        mockNotificationMapper(false);
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        notificationEventServiceDefault.send(context, onboarding, QueueEvent.ADD);
        verifyNoInteractions(eventHubRestClient);
    }

    @Test
    void sendMessageWithTestEnvProducts() {
        final Onboarding onboarding = createOnboarding();
        final Product product = createProduct();
        product.setTestEnvProductIds(List.of("prod-interop-coll", "prod-interop-atst"));
        when(productService.getProduct(any())).thenReturn(product);
        mockNotificationMapper(true);
        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(new InstitutionResponse());
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        notificationEventServiceDefault.send(context, onboarding, QueueEvent.ADD);
        verify(eventHubRestClient, times(9))
                .sendMessage(anyString(), anyString());
    }

    @Test
    void sendMessageWithError() {
        final Onboarding onboarding = createOnboarding();
        final Product product = createProduct();
        when(productService.getProduct(any())).thenReturn(product);
        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(new InstitutionResponse());
        mockNotificationMapper(true);
        doThrow(new NotificationException("Impossible to send notification for object" + onboarding))
                .when(eventHubRestClient).sendMessage(anyString(), anyString());
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        assertThrows(NotificationException.class, () -> notificationEventServiceDefault.send(context, onboarding, QueueEvent.ADD));
        verify(eventHubRestClient, times(1))
                .sendMessage(anyString(), anyString());
    }

    @Test
    void sendMessageNullConsumers() {
        final Onboarding onboarding = createOnboarding();
        Product test = new Product();
        test.setConsumers(List.of());
        when(productService.getProduct(any())).thenReturn(test);
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        notificationEventServiceDefault.send(context, onboarding, QueueEvent.ADD);
        verifyNoInteractions(eventHubRestClient);
    }

    @Test
    @DisplayName("Should return correct notifications to send")
    public void shouldReturnCorrectNotificationsToSend() {
        NotificationToSendFilters filters = new NotificationToSendFilters();
        filters.setPage(1);
        filters.setSize(10);

        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboarding-id");
        Institution institution = new Institution();
        institution.setId("institution-id");
        onboarding.setInstitution(institution);
        List<Onboarding> onboardings = List.of(onboarding);
        PanacheQuery<Onboarding> panacheQuery = mock(PanacheQuery.class);
        when(onboardingRepository.find((Document) any(), any())).thenReturn(panacheQuery);
        when(panacheQuery.page(filters.getPage(), filters.getSize())).thenReturn(panacheQuery);
        when(panacheQuery.list()).thenReturn(onboardings);
        when(onboardingRepository.find((Document) any(), any()).count()).thenReturn((long) onboardings.size());

        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId("institution-id");
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(institutionResponse);

        mockNotificationMapper(true);
        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));

        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        when(queueEventExaminer.determineEventType(any())).thenReturn(QueueEvent.ADD);
        FindNotificationToSendResponse response = notificationEventServiceDefault.findNotificationToSend(context, filters);

        assertEquals(onboardings.size(), response.getCount());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when size is greater than MAX_SIZE")
    public void shouldThrowIllegalArgumentExceptionWhenSizeIsGreaterThanMaxSize() {
        NotificationToSendFilters filters = new NotificationToSendFilters();
        filters.setPage(1);
        filters.setSize(101);

        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        assertThrows(IllegalArgumentException.class, () -> notificationEventServiceDefault.findNotificationToSend(context, filters));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when status is not COMPLETED or DELETED")
    public void shouldThrowIllegalArgumentExceptionWhenStatusIsNotCompletedOrDeleted() {
        NotificationToSendFilters filters = new NotificationToSendFilters();
        filters.setPage(1);
        filters.setSize(10);
        filters.setStatus("INVALID_STATUS");

        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        assertThrows(IllegalArgumentException.class, () -> notificationEventServiceDefault.findNotificationToSend(context, filters));
    }

    private Onboarding createOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboarding.getId());
        String productId = "prod-io";
        onboarding.setProductId(productId);
        onboarding.setPricingPlan("pricingPlan");
        onboarding.setUsers(List.of());
        onboarding.setInstitution(new Institution());
        onboarding.setUserRequestUid("example-uid");

        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("example");
        billing.setVatNumber("example");
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);
        return onboarding;
    }

    private Product createProduct() {
        var product = new Product();
        product.setConsumers(List.of("STANDARD", "SAP", "FD"));
        return product;
    }

    private void mockNotificationMapper(boolean shouldSendNotification) {
        BaseNotificationBuilder notificationMapper = mock(BaseNotificationBuilder.class);
        when(notificationBuilderFactory.create(any())).thenReturn(notificationMapper);
        when(notificationMapper.buildNotificationToSend(any(), any(), any(), any())).thenReturn(new NotificationToSend());
        when(notificationMapper.shouldSendNotification(any(), any())).thenReturn(shouldSendNotification);
    }

}
