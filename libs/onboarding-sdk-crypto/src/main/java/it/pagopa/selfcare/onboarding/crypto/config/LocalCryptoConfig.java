package it.pagopa.selfcare.onboarding.crypto.config;

import java.security.PrivateKey;
import java.security.cert.Certificate;

public class LocalCryptoConfig {

    private Certificate certificate;
    private PrivateKey privateKey;

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }
}
