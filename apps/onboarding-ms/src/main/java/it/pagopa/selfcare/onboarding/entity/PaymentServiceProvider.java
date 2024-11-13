package it.pagopa.selfcare.onboarding.entity;

import lombok.Data;

import java.util.List;

@Data
public class PaymentServiceProvider {
  private String abiCode;
  private String businessRegisterNumber;
  private String legalRegisterNumber;
  private String legalRegisterName;
  private boolean vatNumberGroup;
  private List<String> providerNames;
  private String contractType;
  private String contractId;
}
