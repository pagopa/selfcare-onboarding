package it.pagopa.selfcare.onboarding.service;


public interface NotificationService {

    void sendMailRegistration(String institutionName, String destination, String name, String username, String productName);

    void sendMailRegistrationApprove(String institutionName, String name, String username, String productName, String onboardingId);

    void sendMailOnboardingApprove(String institutionName, String name, String username, String productName, String onboardingId);

    void sendMailRegistrationWithContract(String onboardingId, String destination, String name, String username, String productName);
}
