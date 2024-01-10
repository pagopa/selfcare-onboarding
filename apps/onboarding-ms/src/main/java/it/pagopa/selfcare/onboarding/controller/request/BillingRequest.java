package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BillingRequest {
    @NotEmpty(message = "vatNumber is required")
    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;
}
