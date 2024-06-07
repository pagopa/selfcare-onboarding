package it.pagopa.selfcare.onboarding.utils;

import jakarta.enterprise.context.ApplicationScoped;

import static it.pagopa.selfcare.onboarding.entity.Topic.SC_CONTRACTS_FD;
import static it.pagopa.selfcare.onboarding.entity.Topic.SC_CONTRACTS_SAP;
import static it.pagopa.selfcare.onboarding.entity.Topic.SC_CONTRACTS;
@ApplicationScoped
public class SendNotificationFilterFactory {
    private final OpenNotificationFilter openNotificationFilter;
    private final SapNotificationFilter sapNotificationFilter;

    public SendNotificationFilterFactory(OpenNotificationFilter openNotificationFilter, SapNotificationFilter sapNotificationFilter) {
        this.openNotificationFilter = openNotificationFilter;
        this.sapNotificationFilter = sapNotificationFilter;
    }

    public SendNotificationFilter create(String topic) {
        SendNotificationFilter filter;
        if (SC_CONTRACTS_FD.getValue().equalsIgnoreCase(topic) || SC_CONTRACTS.getValue().equalsIgnoreCase(topic)) {
            filter = openNotificationFilter;
        } else if (SC_CONTRACTS_SAP.getValue().equalsIgnoreCase(topic)) {
            filter = sapNotificationFilter;
        } else {
            throw new IllegalArgumentException("Topic not supported");
        }

        return filter;
    }
}
