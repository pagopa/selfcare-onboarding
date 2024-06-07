package it.pagopa.selfcare.onboarding.utils;


import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static it.pagopa.selfcare.onboarding.entity.Topic.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class SendNotificationFilterFactoryTest {
    @InjectMocks
    private OpenNotificationFilter openNotificationFilter;
    @InjectMocks
    private SapNotificationFilter sapNotificationFilter;
    @Inject
    private SendNotificationFilterFactory sendNotificationFilterFactory;

    @Test
    @DisplayName("Should return OpenFilter for SC_CONTRACTS_FD topic")
    public void shouldReturnOpenFilterForSCContractsFDTopic() {
        SendNotificationFilter result = sendNotificationFilterFactory.create(SC_CONTRACTS_FD.getValue());
        assertTrue(result instanceof OpenNotificationFilter);
    }

    @Test
    @DisplayName("Should return OpenFilter for SC_CONTRACTS topic")
    public void shouldReturnOpenFilterForSCContractsTopic() {
        SendNotificationFilter result = sendNotificationFilterFactory.create(SC_CONTRACTS.getValue());
        assertTrue(result instanceof OpenNotificationFilter);
    }

    @Test
    @DisplayName("Should return SapNotificationFilter for SC_CONTRACTS_SAP topic")
    public void shouldReturnSapNotificationFilterForSCContractsSAPTopic() {
        SendNotificationFilter result = sendNotificationFilterFactory.create(SC_CONTRACTS_SAP.getValue());
        assertTrue(result instanceof SapNotificationFilter);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for unsupported topic")
    public void shouldThrowIllegalArgumentExceptionForUnsupportedTopic() {
        assertThrows(IllegalArgumentException.class, () -> sendNotificationFilterFactory.create("unsupported_topic"));
    }
}