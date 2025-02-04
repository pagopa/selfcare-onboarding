package it.pagopa.selfcare.onboarding.controller.response;

import lombok.Data;

@Data
public class UserOnboardingResponseV1 {

    private String id;
    private PartyRoleV1 role;
    private String productRole;
    private String userMailUuid;
}
