package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
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
        return config.registrationUserPath();
    }

    @Override
    public String getEmailCompletionPath(MailTemplatePathConfig config) {
        return config.completePathUser();
    }

    @Override
    public String getPdfFormatFilename() {
        return null;
    }

    @Override
    public TokenType getTokenType() {
        return TokenType.LEGALS;
    }

    @Override
    public String getConfirmTokenUrl(MailTemplatePlaceholdersConfig config) {
        return config.confirmTokenUserPlaceholder();
    }

    @Override
    public String getRejectTokenUrl(MailTemplatePlaceholdersConfig config) {
        return config.rejectTokenUserPlaceholder();
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
