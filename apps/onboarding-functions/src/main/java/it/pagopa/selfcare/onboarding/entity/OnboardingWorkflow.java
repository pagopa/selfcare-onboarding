package it.pagopa.selfcare.onboarding.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.product.entity.Product;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OnboardingWorkflowAggregator.class, name = "AGGREGATOR"),
        @JsonSubTypes.Type(value = OnboardingWorkflowInstitution.class, name = "INSTITUTION"),
        @JsonSubTypes.Type(value = OnboardingWorkflowUser.class, name = "USER")
})
public abstract class OnboardingWorkflow {

    OnboardingWorkflow(Onboarding onboarding) {
        this.onboarding = onboarding;
    }

    public OnboardingWorkflow() {
    }

    protected Onboarding onboarding;
    public abstract String emailRegistrationPath(MailTemplatePathConfig config);

    public abstract String getEmailCompletionPath(MailTemplatePathConfig config);

    public abstract String getConfirmTokenUrl(MailTemplatePlaceholdersConfig config);

    public abstract String getRejectTokenUrl(MailTemplatePlaceholdersConfig config);

    public abstract String getContractTemplatePath(Product product);

    public Onboarding getOnboarding() {
        return onboarding;
    }

    public void setOnboarding(Onboarding onboarding) {
        this.onboarding = onboarding;
    }

}
