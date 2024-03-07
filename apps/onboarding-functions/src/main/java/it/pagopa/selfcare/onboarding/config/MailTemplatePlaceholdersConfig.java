package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.mail-template.placeholders.onboarding")
public interface MailTemplatePlaceholdersConfig {

    String userName();
    String userSurname();
    String productName();
    String institutionDescription();
    String businessName();

    String adminLink();
    String completeSelfcareName();
    String completeProductName();
    String completeSelfcarePlaceholder();

    String confirmTokenName();
    String confirmTokenPlaceholder();
    String rejectTokenName();
    String rejectTokenPlaceholder();
    String notificationProductName();
    String notificationRequesterName();
    String  notificationRequesterSurname();
    String reasonForReject();

    String rejectOnboardingUrlPlaceholder();
    String rejectOnboardingUrlValue();
    String rejectProductName();


}
