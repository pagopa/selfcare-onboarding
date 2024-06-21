package it.pagopa.selfcare.onboarding.entity;

public class OnboardingWorkflow {
    private String contractTemplatePath;
    private String emailRegistrationPath;
    private Onboarding onboarding;

    public String getContractTemplatePath() {
        return contractTemplatePath;
    }

    public void setContractTemplatePath(String contractTemplatePath) {
        this.contractTemplatePath = contractTemplatePath;
    }

    public Onboarding getOnboarding() {
        return onboarding;
    }

    public void setOnboarding(Onboarding onboarding) {
        this.onboarding = onboarding;
    }

    public void setEmailRegistrationPath(String emailRegistrationPath) {
        this.emailRegistrationPath = emailRegistrationPath;
    }

    public String getEmailRegistrationPath() {
        return emailRegistrationPath;
    }

    public OnboardingWorkflow() {
    }


}
