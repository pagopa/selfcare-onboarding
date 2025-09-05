package it.pagopa.selfcare.onboarding.crypto.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class DataEncryptionUtils {

    private static String defaultKey;

    private static SecretKey getKey(String key) {
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(raw, "AES");
    }

    public static void setDefaultKey(String key) {
        defaultKey = key;
    }

    public static String encrypt(String plain) {
        return encrypt(plain, defaultKey);
    }

    public static String decrypt(String cipherText) {
        return decrypt(cipherText, defaultKey);
    }

    public static String encrypt(String key, String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey(key), new GCMParameterSpec(128, iv));
            byte[] ct = cipher.doFinal(plain.getBytes());

            int ivLen = iv.length;
            int ctLen = ct.length;

            if (ctLen > Integer.MAX_VALUE - ivLen) {
                throw new IllegalArgumentException("Input too large for encryption.");
            }

            int totalLen = ivLen + ctLen;
            byte[] out = new byte[totalLen];
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decrypt(String key, String cipherText) {
        if (cipherText == null) return null;
        try {
            byte[] ivct = Base64.getDecoder().decode(cipherText);
            byte[] iv = java.util.Arrays.copyOfRange(ivct, 0, 12);
            byte[] ct = java.util.Arrays.copyOfRange(ivct, 12, ivct.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(key), new GCMParameterSpec(128, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}