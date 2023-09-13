package it.pagopa.selfcare.controller.request;

import lombok.Data;

@Data
public class BillingRequest {
    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;
}
