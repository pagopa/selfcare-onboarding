package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@QuarkusTest
@TestProfile(MessageServiceDefaultTest.ProductsTopicsProfile.class)
public class MessageServiceDefaultTest {

    @Inject
    MessageServiceDefault messageServiceDefault;

    @RestClient
    @InjectMock
    EventHubRestClient eventHubRestClient;

    public static class ProductsTopicsProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("onboarding-functions.event-hub.map-products-topics", "{\"prod-io\":[\"SC-Contracts\"]}");
        }
    }

    @Test
    void sendMessageWithCustomProduct() {
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId("prod-faked");
        messageServiceDefault.send(onboarding);
        verifyNoInteractions(eventHubRestClient);
    }

    @Test
    void sendMessage() {
        final Onboarding onboarding = createOnboarding();
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        messageServiceDefault.send(onboarding);
        verify(eventHubRestClient, times(1))
                .sendMessage(anyString(), anyString());
    }

    @Test
    void sendMessageWithError() {
        final Onboarding onboarding = createOnboarding();
        doThrow(new RuntimeException()).when(eventHubRestClient).sendMessage(anyString(), anyString());
        messageServiceDefault.send(onboarding);
        verify(eventHubRestClient, times(1))
                .sendMessage(anyString(), anyString());
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

}

