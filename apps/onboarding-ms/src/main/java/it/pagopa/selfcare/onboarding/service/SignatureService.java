package it.pagopa.selfcare.onboarding.service;

import java.io.File;
import java.util.List;

public interface SignatureService {
  void verifySignature(File file, String checksum, List<String> usersTaxCode);

  void verifySignature(File file);

  String digest(File file);
}
