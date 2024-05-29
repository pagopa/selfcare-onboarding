package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.entity.NotificationToSend;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.mapper.NotificationMapper;
import jakarta.enterprise.context.ApplicationScoped;

import static it.pagopa.selfcare.onboarding.entity.Topic.*;

@ApplicationScoped
public class NotificationFactory {

    private final NotificationMapper notificationMapper;

    public NotificationFactory(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    public NotificationToSend create(String topic, Onboarding onboarding) {
        if (SC_CONTRACTS_FD.getValue().equalsIgnoreCase(topic)) {
            return notificationMapper.toSCContractsFD(onboarding);
        } else if (SC_CONTRACTS_SAP.getValue().equalsIgnoreCase(topic)) {
            return notificationMapper.toSCContractsSAP(onboarding);
        } else if (SC_CONTRACTS.getValue().equalsIgnoreCase(topic)) {
            return notificationMapper.toSCContracts(onboarding);
        } else {
            throw new IllegalArgumentException("Topic not supported");
        }
    }

}
