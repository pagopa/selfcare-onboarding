package it.pagopa.selfcare.controller.response;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BillingResponse {
    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;
}
