package it.pagopa.selfcare.onboarding.crypto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;

@ApplicationScoped
public class NamirialPkcs7HashSignServiceImpl implements Pkcs7HashSignService {

    @Inject
    NamirialSignService namirialSignService;


    @Override
    public byte[] sign(InputStream is) {
        return namirialSignService.pkcs7Signhash(is);
    }

}