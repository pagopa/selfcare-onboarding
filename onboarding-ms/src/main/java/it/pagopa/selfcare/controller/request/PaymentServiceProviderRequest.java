package it.pagopa.selfcare.controller.request;

import lombok.Data;

@Data
public class PaymentServiceProviderRequest {
    private String abiCode;
    private String businessRegisterNumber;
    private String legalRegisterNumber;
    private String legalRegisterName;
    private boolean vatNumberGroup;
}
