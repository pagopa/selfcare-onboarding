package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.product.entity.Product;
import java.util.Objects;

public class OnboardingWorkflowUser extends OnboardingWorkflow {

  private static final String PDF_FORMAT_USER_FILENAME = "%s_nomina_referente.pdf";

  private String type;

  public OnboardingWorkflowUser(Onboarding onboarding, String type) {
    super(onboarding);
    this.type = type;
  }

  public OnboardingWorkflowUser() {}

  @Override
  public String emailRegistrationPath(MailTemplatePathConfig config) {
    final String managerId =
        this.onboarding.getUsers().stream()
            .filter(user -> PartyRole.MANAGER == user.getRole())
            .map(User::getId)
            .findAny()
            .orElse(null);
    if (Objects.nonNull(this.onboarding.getPreviousManagerId())
        && this.onboarding.getPreviousManagerId().equals(managerId)) {
      return config.registrationUserPath();
    }
    return config.registrationUserNewManagerPath();
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
    return product.getUserContractTemplate(getIstitutionType()).getContractTemplatePath();
  }

  @Override
  public String getContractTemplateVersion(Product product) {
    return product.getUserContractTemplate(getIstitutionType()).getContractTemplateVersion();
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
