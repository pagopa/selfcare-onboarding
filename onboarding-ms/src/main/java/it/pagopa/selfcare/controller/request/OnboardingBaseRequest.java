package it.pagopa.selfcare.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingBaseRequest {

    @NotEmpty(message = "productId is required")
    private String productId;

    @NotEmpty(message = "at least one user is required")
    private List<UserRequest> users;

    private String pricingPlan;

    private ContractRequest contract;
    private OnboardingImportContract contractImported;

    private Boolean signContract;

}
