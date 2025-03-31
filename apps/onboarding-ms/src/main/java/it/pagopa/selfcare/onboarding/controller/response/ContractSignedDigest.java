package it.pagopa.selfcare.onboarding.controller.response;

import lombok.Data;

@Data
public class ContractSignedDigest {
  private String digest;

  public static ContractSignedDigest digest(String digest) {
    ContractSignedDigest contractSignedReport = new ContractSignedDigest();
    contractSignedReport.setDigest(digest);
    return contractSignedReport;
  }
}
