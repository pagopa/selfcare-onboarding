package it.pagopa.selfcare.onboarding.workflow;

import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.product.entity.Product;

public interface WorkflowExecutorTemplateUser extends WorkflowExecutorTemplate {

    @Override
    default String getContractTemplatePath(Product product, Onboarding onboarding) {
        return null;
    }

    @Override
    default String getEmailRegistrationPath(MailTemplatePathConfig mailTemplatePathConfig) {
        return mailTemplatePathConfig.registrationUserPath();
    }

    @Override
    default String getEmailCompletionPath() {
        return null;
    }
}
