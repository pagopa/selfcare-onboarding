package it.pagopa.selfcare.controller.response;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingResponse {

    private InstitutionResponse institution;

    private String productId;
    private String pricingPlan;
    private List<UserResponse> users;
    private BillingResponse billing;

}
