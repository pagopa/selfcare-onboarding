package it.pagopa.selfcare.onboarding.crypto.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DataEncryptionUtilTest {

    @BeforeEach
    void setup() {
        DataEncryptionUtils.setDefaultKey("0123456789ABCDEF0123456789ABCDEF");
    }

    @Test
    void testEncrypt() {
        final String iban = "ITXXXXX24XXX78XXXX12";
        String encryptedString = DataEncryptionUtils.encrypt(iban);
        Assertions.assertNotNull(encryptedString);
        Assertions.assertFalse(encryptedString.isEmpty());
    }

    @Test
    void testEncryptDecrypt() {
        final String iban = "ITXXXXX24XXX78XXXX12";
        String encryptedString = DataEncryptionUtils.encrypt(iban);
        String decryptedString = DataEncryptionUtils.decrypt(encryptedString);
        Assertions.assertEquals(iban, decryptedString);
    }
}
