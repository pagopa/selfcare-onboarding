package it.pagopa.selfcare.onboarding.controller.response;

import it.pagopa.selfcare.onboarding.entity.AdditionalInformations;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingGet {
    private String id;
    private String productId;
    private String workflowType;
    private InstitutionResponse institution;
    private List<UserResponse> users;
    private String pricingPlan;
    private BillingResponse billing;
    private Boolean signContract;
    private AdditionalInformationsResponse additionalInformations;

    private String status;
    private String userRequestUid;
}
