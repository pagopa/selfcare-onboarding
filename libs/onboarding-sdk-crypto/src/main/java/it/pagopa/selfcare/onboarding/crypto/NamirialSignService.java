package it.pagopa.selfcare.onboarding.crypto;


import jakarta.enterprise.context.ApplicationScoped;

import java.io.InputStream;

@ApplicationScoped
public interface NamirialSignService {
    byte[] pkcs7Signhash(InputStream is);
}