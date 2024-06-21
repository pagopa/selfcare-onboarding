package it.pagopa.selfcare.onboarding.workflow;

import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.product.entity.ContractStorage;
import it.pagopa.selfcare.product.entity.Product;

import java.util.Optional;

public interface WorkflowExecutorTemplateInstitution extends WorkflowExecutorTemplate {

    @Override
    default String getContractTemplatePath(Product product, Onboarding onboarding) {
        return Optional.ofNullable(product.getInstitutionContractMappings())
                .filter(mappings -> mappings.containsKey(onboarding.getInstitution().getInstitutionType()))
                .map(mappings -> mappings.get(onboarding.getInstitution().getInstitutionType()))
                .map(ContractStorage::getContractTemplatePath)
                .orElse(product.getContractTemplatePath());
    }

    @Override
    default String getEmailRegistrationPath(MailTemplatePathConfig mailTemplatePathConfig) {
        return mailTemplatePathConfig.registrationRequestPath();
    }

    @Override
    default String getEmailCompletionPath() {
        return null;
    }
}
