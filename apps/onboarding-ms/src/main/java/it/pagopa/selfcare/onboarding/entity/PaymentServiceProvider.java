package it.pagopa.selfcare.onboarding.entity;

import lombok.Data;

@Data
public class PaymentServiceProvider {
    private String abiCode;
    private String businessRegisterNumber;
    private String legalRegisterNumber;
    private String legalRegisterName;
    private boolean vatNumberGroup;
}
