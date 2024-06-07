package it.pagopa.selfcare.onboarding.controller.request;


import it.pagopa.selfcare.onboarding.common.InstitutionType;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingUserRequest {

    @NotEmpty(message = "productId is required")
    private String productId;

    @NotEmpty(message = "origin is required")
    private String origin;

    @NotEmpty(message = "originId is required")
    private String originId;

    private String taxCode;

    private String subunitCode;

    private InstitutionType institutionType;

    @NotEmpty(message = "at least one user is required")
    private List<UserRequest> users;

}
