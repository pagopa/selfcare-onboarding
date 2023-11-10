package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.mail-template.path.onboarding")
public interface MailTemplatePathConfig {

    String completePath();
    String completePathFd();

    String autocompletePath();

    String delegationNotificationPath();

    String registrationPath();

    String notificationPath();

    String rejectPath();
    String registrationRequestPath();
    String registrationNotificationAdminPath();
}
