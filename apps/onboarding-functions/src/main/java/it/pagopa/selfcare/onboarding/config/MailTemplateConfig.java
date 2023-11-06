package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.mail-template.placeholders.onboarding")
public interface MailTemplateConfig {

    String completePath();
    String completeProductName();
    String completeSelfcarePlaceholder();
    String completeSelfcareName();
    String completePathFd();

    String autocompletePath();

    String delegationNotificationPath();

    String path();
    String userName();
    String userSurname();
    String productName();
    String institutionDescription();

    String confirmTokenName();
    String confirmTokenPlaceholder();
    String rejectTokenName();
    String rejectTokenPlaceholder();
    String adminLink();

    String notificationPath();
    String notificationAdminEmail();
    String notificationProductName();
    String notificationRequesterName();
    String notificationRequesterSurname();

    String rejectPath();
    String rejectProductName();
    String rejectOnboardingUrlPlaceholder();
    String rejectOnboardingUrlValue();

    String registrationRequestPath();
    String registrationNotificationAdminPath();
}
