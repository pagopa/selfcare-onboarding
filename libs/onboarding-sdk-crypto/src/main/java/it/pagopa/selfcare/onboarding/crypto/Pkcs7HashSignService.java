package it.pagopa.selfcare.onboarding.crypto;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;

public interface Pkcs7HashSignService extends SignatureInterface {

    boolean returnsFullPdf();
}
