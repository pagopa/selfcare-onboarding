package it.pagopa.selfcare.onboarding.utils;

import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.selfcare.onboarding.mapper.NotificationMapper;
import it.pagopa.selfcare.onboarding.mapper.impl.NotificationCommonMapper;
import it.pagopa.selfcare.onboarding.mapper.impl.NotificationFdMapper;
import it.pagopa.selfcare.onboarding.mapper.impl.NotificationSapMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static it.pagopa.selfcare.onboarding.entity.Topic.*;

public class NotificationMapperFactoryTest {
    @InjectMocks
    private NotificationMapperFactory notificationMapperFactory;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void createReturnsFdMapperForFdTopic() {
        NotificationMapper result = notificationMapperFactory.create(SC_CONTRACTS_FD.getValue());
        assertTrue(result instanceof NotificationFdMapper);
    }

    @Test
    public void createReturnsSapMapperForSapTopic() {
        NotificationMapper result = notificationMapperFactory.create(SC_CONTRACTS_SAP.getValue());
        assertTrue(result instanceof NotificationSapMapper);
    }

    @Test
    public void createReturnsCommonMapperForCommonTopic() {
        NotificationMapper result = notificationMapperFactory.create(SC_CONTRACTS.getValue());
        assertTrue(result instanceof NotificationCommonMapper);
    }

    @Test
    public void createThrowsExceptionForUnsupportedTopic() {
        assertThrows(IllegalArgumentException.class, () -> notificationMapperFactory.create("unsupported_topic"));
    }

}