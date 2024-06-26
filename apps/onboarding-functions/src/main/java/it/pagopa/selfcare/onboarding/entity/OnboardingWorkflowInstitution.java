package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.product.entity.ContractStorage;
import it.pagopa.selfcare.product.entity.Product;

import java.util.Optional;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_FD;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_FD_GARANTITO;

public class OnboardingWorkflowInstitution extends OnboardingWorkflow {

    private String type;

    public OnboardingWorkflowInstitution() {
    }

    public OnboardingWorkflowInstitution(Onboarding onboarding, String type) {
        super(onboarding);
        this.type = type;
    }

    @Override
    public String emailRegistrationPath(MailTemplatePathConfig config) {
        return config.registrationPath();
    }

    @Override
    public String getEmailCompletionPath(MailTemplatePathConfig config) {
        if (InstitutionType.PT.equals(this.onboarding.getInstitution().getInstitutionType())) {
            return config.completePathPt();
        } else {
            return this.onboarding.getProductId().equals(PROD_FD.getValue()) || this.onboarding.getProductId().equals(PROD_FD_GARANTITO.getValue())
                    ? config.completePathFd()
                    : config.completePath();
        }
    }

    @Override
    public String getConfirmTokenUrl(MailTemplatePlaceholdersConfig config) {
        return config.confirmTokenPlaceholder();
    }

    @Override
    public String getRejectTokenUrl(MailTemplatePlaceholdersConfig config) {
        return config.rejectTokenPlaceholder();
    }

    @Override
    public String getContractTemplatePath(Product product) {

        return Optional.ofNullable(product.getInstitutionContractMappings())
                .filter(mappings -> mappings.containsKey(onboarding.getInstitution().getInstitutionType()))
                .map(mappings -> mappings.get(onboarding.getInstitution().getInstitutionType()))
                .map(ContractStorage::getContractTemplatePath)
                .orElse(product.getContractTemplatePath());
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
