package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.product.entity.ContractStorage;
import it.pagopa.selfcare.product.entity.Product;

import java.util.Objects;
import java.util.Optional;

public class OnboardingWorkflowAggregator extends OnboardingWorkflow {

    private String type;

    public OnboardingWorkflowAggregator(){}

    public OnboardingWorkflowAggregator(Onboarding onboarding, String type) {
        super(onboarding);
        this.type = type;
    }

    @Override
    public TokenType getTokenType() {
        return TokenType.INSTITUTION;
    }

    @Override
    public String getPdfFormatFilename() {
        return PDF_FORMAT_FILENAME;
    }

    @Override
    public String emailRegistrationPath(MailTemplatePathConfig config) {
        return config.registrationAggregatorPath();
    }

    @Override
    public String getEmailCompletionPath(MailTemplatePathConfig config) {
        return config.completePath();
    }

    @Override
    public String getContractTemplatePath(Product product) {
        if(Objects.isNull(onboarding.getInstitution()) || Objects.isNull(onboarding.getInstitution().getInstitutionType())){
            return null;
        }

        return Optional.ofNullable(product.getInstitutionContractMappings())
                .filter(mappings -> mappings.containsKey(onboarding.getInstitution().getInstitutionType().name()))
                .map(mappings -> mappings.get(onboarding.getInstitution().getInstitutionType().name()))
                .map(ContractStorage::getContractTemplatePath)
                .orElse(product.getContractTemplatePath());
    }

    @Override
    public String getContractTemplateVersion(Product product) {
        return product.getContractTemplateVersion();
    }

    @Override
    public String getConfirmTokenUrl(MailTemplatePlaceholdersConfig config) {
        return config.confirmTokenPlaceholder();
    }

    @Override
    public String getRejectTokenUrl(MailTemplatePlaceholdersConfig config) {
        return config.rejectTokenPlaceholder();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
