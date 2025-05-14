package it.pagopa.selfcare.onboarding.steps;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class IntegrationObjectUtils {

  static Onboarding createDummyOnboarding() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId(UUID.fromString("89ad7142-24bb-48ad-8504-9c9231137e85").toString());
    onboarding.setProductId("prod-pagopa");
    onboarding.setCreatedAt(LocalDateTime.now());
    onboarding.setWorkflowType(WorkflowType.FOR_APPROVE_GPU);
    onboarding.setStatus(OnboardingStatus.REQUEST);

    Institution institution = new Institution();
    institution.setTaxCode("taxCode");
    institution.setSubunitCode("subunitCode");
    institution.setInstitutionType(InstitutionType.PSP);
    onboarding.setInstitution(institution);

    Billing billing = new Billing();
    billing.setRecipientCode("RC000");
    onboarding.setBilling(billing);

    User user = new User();
    user.setId("actual-user-id");
    user.setRole(PartyRole.MANAGER);
    onboarding.setUsers(List.of(user));

    return onboarding;
  }

  static Onboarding createOnboardingForConflictScenario() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId(UUID.randomUUID().toString());
    onboarding.setProductId("prod-io");
    onboarding.setStatus(OnboardingStatus.COMPLETED);
    onboarding.setCreatedAt(LocalDateTime.now());
    onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);

    Institution institution = new Institution();
    institution.setOrigin(Origin.IPA);
    institution.setOriginId("c_l186");
    institution.setDescription("Comune di Tocco da Casauria");
    institution.setTaxCode("00231830688");
    institution.setInstitutionType(InstitutionType.PA);
    onboarding.setInstitution(institution);

    Billing billing = new Billing();
    billing.setRecipientCode("UFD333");
    onboarding.setBilling(billing);

    User user = new User();
    user.setId("actual-user-id");
    user.setRole(PartyRole.MANAGER);
    onboarding.setUsers(List.of(user));

    return onboarding;
  }

  static Token createDummyToken() {
    Token token = new Token();
    token.setId(UUID.fromString("89ad7142-24bb-48ad-8504-9c9231137e85").toString());
    token.setProductId("prod-pagopa");
    token.setCreatedAt(LocalDateTime.now());
    return token;
  }
}
