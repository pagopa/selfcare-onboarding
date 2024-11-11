package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.utils.InstitutionUtils;
import it.pagopa.selfcare.onboarding.utils.UserUtils;
import it.pagopa.selfcare.product.entity.Product;

public class OnboardingWorkflowUserEa extends OnboardingWorkflow {

  private static final String PDF_FORMAT_USER_FILENAME = "%s_nomina_referente.pdf";

  private String type;

  public OnboardingWorkflowUserEa(Onboarding onboarding, String type) {
    super(onboarding);
    this.type = type;
  }

  public OnboardingWorkflowUserEa() {}

  @Override
  public String getEmailRegistrationPath(MailTemplatePathConfig config) {
    return UserUtils.getEmailRegistrationTemplatePath(config, this.onboarding);
  }

  @Override
  public String getEmailCompletionPath(MailTemplatePathConfig config) {
    return config.completePathUser();
  }

  @Override
  public String getPdfFormatFilename() {
    return PDF_FORMAT_USER_FILENAME;
  }

  @Override
  public TokenType getTokenType() {
    return TokenType.USER;
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
    return product
        .getUserAggregatorContractTemplate(InstitutionUtils.getCurrentInstitutionType(onboarding))
        .getContractTemplatePath();
  }

  @Override
  public String getContractTemplateVersion(Product product) {
    return product
        .getUserAggregatorContractTemplate(InstitutionUtils.getCurrentInstitutionType(onboarding))
        .getContractTemplateVersion();
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
