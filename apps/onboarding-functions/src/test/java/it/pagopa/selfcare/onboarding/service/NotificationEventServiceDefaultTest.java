package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.mapper.impl.NotificationCommonMapper;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.onboarding.utils.NotificationMapperFactory;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

import java.util.List;
import java.util.Optional;

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
    TokenRepository tokenRepository;

    @RestClient
    @InjectMock
    InstitutionApi institutionApi;

    @Test
    void sendMessage() {
        final Onboarding onboarding = createOnboarding();
        final Product product = createProduct();
        when(productService.getProduct(any())).thenReturn(product);
        NotificationCommonMapper notificationMapper = mock(NotificationCommonMapper.class);
        when(notificationMapperFactory.create(anyString())).thenReturn(notificationMapper);
        when(notificationMapper.toNotificationToSend(any(), any(), any(), any())).thenReturn(new NotificationToSend());

        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(new InstitutionResponse());
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        messageServiceDefault.send(onboarding, QueueEvent.ADD);
        verify(eventHubRestClient, times(3))
                .sendMessage(anyString(), anyString());
    }

    @Test
    void sendMessageWithoutToken() {
        final Onboarding onboarding = createOnboarding();
        final Product product = createProduct();
        when(productService.getProduct(any())).thenReturn(product);
        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.empty());
        messageServiceDefault.send(onboarding, QueueEvent.ADD);
        verifyNoInteractions(eventHubRestClient);
    }

    @Test
    void sendMessageWithError() {
        final Onboarding onboarding = createOnboarding();
        final Product product = createProduct();
        when(productService.getProduct(any())).thenReturn(product);
        NotificationCommonMapper notificationMapper = mock(NotificationCommonMapper.class);
        when(notificationMapperFactory.create(anyString())).thenReturn(notificationMapper);
        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(new InstitutionResponse());
        when(notificationMapper.toNotificationToSend(any(), any(), any(), any())).thenReturn(new NotificationToSend());
        doThrow(new NotificationException("Impossible to send notification for object" + onboarding))
                .when(eventHubRestClient).sendMessage(anyString(), anyString());
        assertThrows(NotificationException.class, () -> messageServiceDefault.send(onboarding, QueueEvent.ADD));
        verify(eventHubRestClient, times(1))
                .sendMessage(anyString(), anyString());
    }

    @Test
    void sendMessageNullConsumers() {
        final Onboarding onboarding = createOnboarding();
        Product test = new Product();
        test.setConsumers(List.of());
        when(productService.getProduct(any())).thenReturn(test);
        messageServiceDefault.send(onboarding, QueueEvent.ADD);
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

