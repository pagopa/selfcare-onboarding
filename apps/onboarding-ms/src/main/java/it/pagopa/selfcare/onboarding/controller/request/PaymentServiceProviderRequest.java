package it.pagopa.selfcare.onboarding.controller.request;

import lombok.Data;

import java.util.List;

@Data
public class PaymentServiceProviderRequest {
  private String abiCode;
  private String businessRegisterNumber;
  private String legalRegisterNumber;
  private String legalRegisterName;
  private boolean vatNumberGroup;
  private List<String> providerNames;
  private String contractType;
  private String contractId;
}
