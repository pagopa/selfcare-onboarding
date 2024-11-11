package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;

import java.util.Objects;

public class UserUtils {

  public UserUtils() {}

  public static String getEmailRegistrationTemplatePath(MailTemplatePathConfig config, Onboarding onboarding) {
    final String managerId =
            onboarding.getUsers().stream()
                    .filter(user -> PartyRole.MANAGER == user.getRole())
                    .map(User::getId)
                    .findAny()
                    .orElse(null);
    if (Objects.nonNull(onboarding.getPreviousManagerId())
            && onboarding.getPreviousManagerId().equals(managerId)) {
      return config.registrationUserPath();
    }
    return config.registrationUserNewManagerPath();
  }
}
