package it.pagopa.selfcare.onboarding.event.entity;

import lombok.Data;

import java.util.List;

@Data
public class PaymentServiceProvider {
  private String abiCode;
  private String businessRegisterNumber;
  private String legalRegisterNumber;
  private String legalRegisterName;
  private boolean vatNumberGroup;
  private String contractType;
  private String contractId;
  private List<String> providerNames;
}
