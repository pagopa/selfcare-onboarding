package it.pagopa.selfcare.service;


public interface NotificationService {

    void sendMailWithContract(String onboardingId, String filenameContract, String destination, String name, String username, String productName, String token);
}
