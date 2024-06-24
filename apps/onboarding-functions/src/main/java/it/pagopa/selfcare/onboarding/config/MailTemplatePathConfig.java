package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.mail-template.path.onboarding")
public interface MailTemplatePathConfig {

    String completePath();
    String completePathFd();
    String completePathPt();
    String completePathUser();
    String completePathAggregator();

    String autocompletePath();

    String delegationNotificationPath();

    String registrationPath();
    String registrationUserPath();
    String registrationAggregatorPath();

    String onboardingApprovePath();

    String rejectPath();
    String registrationRequestPath();
    String registrationApprovePath();
}
