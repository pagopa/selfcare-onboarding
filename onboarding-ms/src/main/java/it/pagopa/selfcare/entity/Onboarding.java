package it.pagopa.selfcare.entity;

import it.pagopa.selfcare.controller.request.ContractRequest;
import it.pagopa.selfcare.controller.request.OnboardingImportContract;
import lombok.Data;

import java.util.List;

@Data
public class Onboarding {

    private String productId;
    private Institution institution;
    private List<User> users;
    private String pricingPlan;
    private Billing billing;
    private ContractRequest contract;
    private OnboardingImportContract contractImported;
    private Boolean signContract;
}
