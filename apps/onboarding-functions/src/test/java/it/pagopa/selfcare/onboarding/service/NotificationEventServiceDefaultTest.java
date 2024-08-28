package it.pagopa.selfcare.onboarding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.dto.*;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.utils.BaseNotificationBuilder;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.onboarding.utils.NotificationBuilderFactory;
import it.pagopa.selfcare.onboarding.utils.QueueEventExaminer;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static it.pagopa.selfcare.onboarding.TestUtils.getMockedContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class NotificationEventServiceDefaultTest {

    @Inject
    NotificationEventServiceDefault notificationServiceDefault;

    @InjectMock
    ProductService productService;

    @RestClient
    @InjectMock
    EventHubRestClient eventHubRestClient;

    @InjectMock
    NotificationBuilderFactory notificationBuilderFactory;

    @InjectMock
    TokenRepository tokenRepository;

    @RestClient
    @InjectMock
    InstitutionApi institutionApi;

    @InjectMock
    QueueEventExaminer queueEventExaminer;


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
        notificationServiceDefault.send(context, onboarding, QueueEvent.ADD);
        verify(eventHubRestClient, times(3))
                .sendMessage(anyString(), anyString());
    }

    @Test
    void sendMessageWithoutQueueEvent() {
        final Onboarding onboarding = createOnboarding();
        final Product product = createProduct();
        when(productService.getProduct(any())).thenReturn(product);
        mockNotificationMapper(true);
        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(new InstitutionResponse());
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        when(queueEventExaminer.determineEventType(any())).thenReturn(QueueEvent.ADD);
        notificationServiceDefault.send(context, onboarding, null);
        verify(eventHubRestClient, times(3))
                .sendMessage(anyString(), anyString());
    }

    private void mockNotificationMapper(boolean shouldSendNotification) {
        BaseNotificationBuilder notificationMapper = mock(BaseNotificationBuilder.class);
        when(notificationBuilderFactory.create(any())).thenReturn(notificationMapper);
        when(notificationMapper.buildNotificationToSend(any(), any(), any(), any())).thenReturn(new NotificationToSend());
        when(notificationMapper.shouldSendNotification(any(), any())).thenReturn(shouldSendNotification);
    }

    @Test
    void sendMessageWithoutToken() {
        final Onboarding onboarding = createOnboarding();
        onboarding.setId("123");
        final Product product = createProduct();
        when(productService.getProduct(any())).thenReturn(product);
        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.empty());
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(new InstitutionResponse());
        mockNotificationMapper(true);
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        notificationServiceDefault.send(context, onboarding, QueueEvent.ADD);
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
        notificationServiceDefault.send(context, onboarding, QueueEvent.ADD);
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
        notificationServiceDefault.send(context, onboarding, QueueEvent.ADD);
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
        assertThrows(NotificationException.class, () -> notificationServiceDefault.send(context, onboarding, QueueEvent.ADD));
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
        notificationServiceDefault.send(context, onboarding, QueueEvent.ADD);
        verifyNoInteractions(eventHubRestClient);
    }

    @Test
    void sendMessageWontProceedsWhenOnboardingIsNotReferredToInstitution() {
        final Onboarding onboarding = createOnboarding();
        onboarding.setWorkflowType(WorkflowType.USERS);

        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        notificationServiceDefault.send(context, onboarding, QueueEvent.ADD);
        verifyNoInteractions(productService);
        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(institutionApi);
        verifyNoInteractions(eventHubRestClient);
    }

    @Test
    void notificationEventMapTest() {
        NotificationToSend notificationToSend =  new NotificationToSend();
        notificationToSend.setId("id");
        notificationToSend.setInternalIstitutionID("internal");
        notificationToSend.setProduct("prod");
        notificationToSend.setState("state");
        notificationToSend.setFileName("fileName");
        notificationToSend.setFilePath("filePath");
        notificationToSend.setContentType("contentType");

        InstitutionToNotify institution = new InstitutionToNotify();
        institution.setDescription("description");
        institution.setInstitutionType(InstitutionType.SA);
        institution.setDigitalAddress("mail");
        notificationToSend.setInstitution(institution);

        BillingToSend billing = new BillingToSend();
        billing.setRecipientCode("12345");
        billing.setPublicService(false);
        notificationToSend.setBilling(billing);

        Map<String, String> properties = NotificationEventServiceDefault.notificationEventMap(notificationToSend, "topic", "traceId");
        assertNotNull(properties);
        assertEquals(properties.get("notificationEventTraceId"), "traceId");
        assertEquals(properties.get("id"), "id");
        assertEquals(properties.get("internalIstitutionID"), "internal");
        assertEquals(properties.get("product"), "prod");
        assertEquals(properties.get("state"), "state");
        assertEquals(properties.get("fileName"), "fileName");
        assertEquals(properties.get("filePath"), "filePath");
        assertEquals(properties.get("contentType"), "application/octet-stream");

        assertEquals(properties.get("description"), "description");
        assertEquals(properties.get("digitalAddress"), "mail");
        assertEquals(properties.get("institutionType"), "SA");

        assertEquals(properties.get("billing.recipientCode"), "12345");
        assertEquals(properties.get("billing.isPublicService"), "false");
    }

    @Test
    void notificationEventMapRootParentTest() {
        NotificationToSend notificationToSend =  new NotificationToSend();
        notificationToSend.setId("id");
        notificationToSend.setInternalIstitutionID("internal");
        notificationToSend.setProduct("prod");
        notificationToSend.setState("state");
        notificationToSend.setFileName("fileName");
        notificationToSend.setFilePath("filePath");
        notificationToSend.setContentType("contentType");

        InstitutionToNotify institution = new InstitutionToNotify();
        institution.setDescription("description");
        institution.setInstitutionType(InstitutionType.SA);
        institution.setDigitalAddress("mail");
        RootParent rootParent = new RootParent();
        rootParent.setDescription("RootDescription");
        rootParent.setId("RootId");
        rootParent.setOriginId("RootOriginId");
        institution.setRootParent(rootParent);
        notificationToSend.setInstitution(institution);

        BillingToSend billing = new BillingToSend();
        billing.setRecipientCode("12345");
        billing.setPublicService(true);
        billing.setVatNumber("123");
        billing.setTaxCodeInvoicing("456");
        notificationToSend.setBilling(billing);

        Map<String, String> properties = NotificationEventServiceDefault.notificationEventMap(notificationToSend, "topic", null);
        assertNotNull(properties);
        assertEquals(properties.get("id"), "id");
        assertEquals(properties.get("internalIstitutionID"), "internal");
        assertEquals(properties.get("product"), "prod");
        assertEquals(properties.get("state"), "state");
        assertEquals(properties.get("fileName"), "fileName");
        assertEquals(properties.get("filePath"), "filePath");
        assertEquals(properties.get("contentType"), "application/octet-stream");

        assertEquals(properties.get("description"), "description");
        assertEquals(properties.get("digitalAddress"), "mail");
        assertEquals(properties.get("institutionType"), "SA");
        assertEquals(properties.get("root.parentId"), "RootId");
        assertEquals(properties.get("root.parentDescription"), "RootDescription");
        assertEquals(properties.get("root.parentOriginId"), "RootOriginId");

        assertEquals(properties.get("billing.recipientCode"), "12345");
        assertEquals(properties.get("billing.isPublicService"), "true");
        assertEquals(properties.get("billing.VatNumber"), "123");
        assertEquals(properties.get("billing.TaxCodeInvoicing"), "456");
    }

    @Test
    void sendNotificationsJsonError() {
        final Onboarding onboarding = createOnboarding();
        final Product product = createProduct();
        when(productService.getProduct(any())).thenReturn(product);

        when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
        when(institutionApi.retrieveInstitutionByIdUsingGET(any())).thenReturn(new InstitutionResponse());

        BaseNotificationBuilder notificationMapper = mock(BaseNotificationBuilder.class);
        when(notificationBuilderFactory.create(any())).thenReturn(notificationMapper);

        NotificationToSend mockNotificationToSend = mock(NotificationToSend.class);
        when(mockNotificationToSend.toString()).thenReturn(mockNotificationToSend.getClass().getName());

        when(notificationMapper.buildNotificationToSend(any(), any(), any(), any())).thenReturn(mockNotificationToSend);
        when(notificationMapper.shouldSendNotification(any(), any())).thenReturn(true);

        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        TelemetryClient telemetryClient = mock(TelemetryClient.class);
        doNothing().when(telemetryClient).trackEvent(anyString(), any(), any());

        assertThrows(RuntimeException.class, () -> notificationServiceDefault.send(context, onboarding, QueueEvent.ADD));
    }

    private Onboarding createOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);
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