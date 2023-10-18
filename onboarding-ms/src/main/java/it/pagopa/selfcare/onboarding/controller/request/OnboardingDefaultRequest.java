package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class OnboardingDefaultRequest extends OnboardingBaseRequest {

    @NotNull(message = "institutionData is required")
    @Valid
    private InstitutionBaseRequest institution;
    @NotNull(message = "billing is required")
    @Valid
    private BillingRequest billing;

}
