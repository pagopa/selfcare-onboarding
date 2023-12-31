package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BillingPaRequest {
    @NotEmpty(message = "vatNumber is required")
    private String vatNumber;
    @NotEmpty(message = "recipientCode is required")
    private String recipientCode;
    private boolean publicServices;
}
