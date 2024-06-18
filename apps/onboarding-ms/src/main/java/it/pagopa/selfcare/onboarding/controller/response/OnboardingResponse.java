package it.pagopa.selfcare.onboarding.controller.response;

import it.pagopa.selfcare.onboarding.controller.request.AdditionalInformationsDto;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingResponse {

    private String id;
    private String productId;
    private String workflowType;
    private InstitutionResponse institution;
    private String pricingPlan;
    private List<UserOnboardingResponse> users;
    private BillingResponse billing;
    private String status;
    private AdditionalInformationsDto additionalInformations;
    private String userRequestUid;
    private Boolean isAggregator;

}
