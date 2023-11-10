package it.pagopa.selfcare.onboarding.service;


public interface NotificationService {

    void sendMailRegistration(String institutionName, String destination, String name, String username, String productName);

    void sendMailRegistrationWithContract(String onboardingId, String destination, String name, String username, String productName, String token);
}
