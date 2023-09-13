package it.pagopa.selfcare.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BillingRequest {
    @NotEmpty(message = "vatNumber is required")
    private String vatNumber;
    @NotEmpty(message = "recipientCode is required")
    private String recipientCode;
    private boolean publicServices;
}
