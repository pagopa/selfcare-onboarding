package it.pagopa.selfcare.controller.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnboardingInfoResponse {
    private String userId;
    private List<OnboardingResponse> institutions;
}
