package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.product.entity.Product;

public class OnboardingWorkflowUser extends OnboardingWorkflow {

    private String type;

    public OnboardingWorkflowUser(Onboarding onboarding, String type) {
        super(onboarding);
        this.type = type;
    }

    public OnboardingWorkflowUser() {
    }


    @Override
    public String emailRegistrationPath(MailTemplatePathConfig config) {
        return config.completePathUser();
    }

    @Override
    public String getEmailCompletionPath(MailTemplatePathConfig config) {
        return config.completePathUser();
    }

    @Override
    public String getContractTemplatePath(Product product) {
        return product.getUserContractTemplatePath();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
