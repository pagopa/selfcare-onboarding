package it.pagopa.selfcare.onboarding.service;


import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.product.entity.Product;

import java.util.List;

public interface NotificationService {

    void sendMailRegistration(String institutionName, String destination, String name, String username, String productName);

    void sendMailRegistrationApprove(String institutionName, String name, String username, String productName, String onboardingId);

    void sendMailOnboardingApprove(String institutionName, String name, String username, String productName, String onboardingId);

    void sendMailRegistrationForContract(String onboardingId, String destination, String name, String username, String productName, String institutionName, String templatePath, String confirmTokenUrl);

    void sendMailRegistrationForContract(String onboardingId, String destination, OnboardingService.SendMailInput sendMailInput, String templatePath, String confirmTokenUrl);

    void sendMailRegistrationForContractAggregator(String onboardingId, String destination, String name, String username, String productName);

    void sendCompletedEmail(String institutionName, List<String> destinationMails, Product product, InstitutionType institutionType, OnboardingWorkflow onboardingWorkflow);

    void sendMailRejection(List<String> destinationMails, Product product, String reasonForReject);

    void sendCompletedEmailAggregate(String institutionName, List<String> destinationMails);

}
