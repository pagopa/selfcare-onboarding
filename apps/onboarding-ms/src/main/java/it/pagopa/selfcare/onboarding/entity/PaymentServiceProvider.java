package it.pagopa.selfcare.onboarding.entity;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentServiceProvider extends BusinessData {
  private String abiCode;
  private boolean vatNumberGroup;
  private List<String> providerNames;
  private String contractType;
  private String contractId;
}