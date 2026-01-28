package it.pagopa.selfcare.onboarding.service;

import java.io.File;
import java.util.List;

public interface SignatureService {
  void verifySignature(File file, String checksum, List<String> usersTaxCode);

  boolean verifySignature(File file);

  File extractFile(File contract);

  String getTemplateAndVerifyDigest(File uploadedFile, File templateFile, boolean skipDigestCheck);
}
