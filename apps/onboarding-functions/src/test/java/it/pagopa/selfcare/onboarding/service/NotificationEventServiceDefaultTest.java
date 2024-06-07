package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.mapper.impl.NotificationCommonMapper;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.onboarding.utils.NotificationMapperFactory;
import it.pagopa.selfcare.onboarding.utils.SendNotificationFilter;
import it.pagopa.selfcare.onboarding.utils.SendNotificationFilterFactory;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@QuarkusTest
public class NotificationEventServiceDefaultTest {

    @Inject
    NotificationEventServiceDefault messageServiceDefault;

    @InjectMock
    ProductService productService;

    @RestClient
    @InjectMock
    EventHubRestClient eventHubRestClient;

    @InjectMock
    NotificationMapperFactory notificationMapperFactory;

    @InjectMock
    SendNotificationFilterFactory sendNotificationFilterFactory;

    @InjectMock
    TokenRepository tokenRepository;

    @RestClient
    @InjectMock
    InstitutionApi institutionApi;


    @Test
    void sendMessage() {
        final Onboarding onboarding = createOnboarding();
        final Product product = createProduct();
        when(productService.getProduct(any())).thenReturn(product);
        mockNotificationMapper();
        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(new InstitutionResponse());
        mockSendNotificationFilter(true);
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        messageServiceDefault.send(context, onboarding, QueueEvent.ADD);
        verify(eventHubRestClient, times(3))
                .sendMessage(anyString(), anyString());
    }

    private void mockNotificationMapper() {
        NotificationCommonMapper notificationMapper = mock(NotificationCommonMapper.class);
        when(notificationMapperFactory.create(anyString())).thenReturn(notificationMapper);
        when(notificationMapper.toNotificationToSend(any(), any(), any(), any())).thenReturn(new NotificationToSend());
    }

    private void mockSendNotificationFilter(boolean shouldSend) {
        SendNotificationFilter sendNotificationFilter = mock(SendNotificationFilter.class);
        when(sendNotificationFilterFactory.create(anyString())).thenReturn(sendNotificationFilter);
        when(sendNotificationFilter.shouldSendNotification(any(), any())).thenReturn(shouldSend);
    }

    @Test
    void sendMessageWithoutToken() {
        final Onboarding onboarding = createOnboarding();
        final Product product = createProduct();
        when(productService.getProduct(any())).thenReturn(product);
        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.empty());
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(new InstitutionResponse());
        mockNotificationMapper();
        mockSendNotificationFilter(true);
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        messageServiceDefault.send(context, onboarding, QueueEvent.ADD);
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
        mockNotificationMapper();
        mockSendNotificationFilter(false);
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        messageServiceDefault.send(context, onboarding, QueueEvent.ADD);
        verifyNoInteractions(eventHubRestClient);
    }

    @Test
    void sendMessageWithTestEnvProducts() {
        final Onboarding onboarding = createOnboarding();
        final Product product = createProduct();
        product.setTestEnvProductIds(List.of("prod-interop-coll", "prod-interop-atst"));
        when(productService.getProduct(any())).thenReturn(product);
        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(new InstitutionResponse());
        mockNotificationMapper();
        mockSendNotificationFilter(true);
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        messageServiceDefault.send(context, onboarding, QueueEvent.ADD);
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
        mockNotificationMapper();
        mockSendNotificationFilter(true);
        doThrow(new NotificationException("Impossible to send notification for object" + onboarding))
                .when(eventHubRestClient).sendMessage(anyString(), anyString());
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        assertThrows(NotificationException.class, () -> messageServiceDefault.send(context, onboarding, QueueEvent.ADD));
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
        messageServiceDefault.send(context, onboarding, QueueEvent.ADD);
        verifyNoInteractions(eventHubRestClient);
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

}
