package it.pagopa.selfcare.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingRequest {

    @NotEmpty(message = "productId is required")
    private String productId;

    @NotEmpty(message = "at least one user is required")
    private List<User> users;

    @NotNull(message = "institutionData is required")
    @Valid
    private InstitutionRequest institution;

    private String pricingPlan;
    private BillingRequest billing;
    private ContractRequest contract;
    private OnboardingImportContract contractImported;

    private Boolean signContract;

}
