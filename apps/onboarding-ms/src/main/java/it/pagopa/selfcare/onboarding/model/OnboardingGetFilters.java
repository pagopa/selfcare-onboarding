package it.pagopa.selfcare.onboarding.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class OnboardingGetFilters {
    private String productId;
    private String institutionId;
    private String onboardingId;
    private String taxCode;
    private String status;
    private String from;
    private String to;
    private Integer page;
    private Integer size;
}
