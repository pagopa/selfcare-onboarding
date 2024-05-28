package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

import java.util.Map;

@ConfigMapping(prefix = "notification")
public interface NotificationConfig {

    Map<String, Consumer> consumers();

    interface Consumer {
        String topic();
        String name();
        String key();
    }

}
