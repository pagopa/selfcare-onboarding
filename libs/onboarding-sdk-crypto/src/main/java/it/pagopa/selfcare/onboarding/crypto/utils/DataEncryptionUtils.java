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

    public static String encrypt(String plain, String key) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey(key), new GCMParameterSpec(128, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            // concateno IV + ciphertext
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);

            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String cipherText, String key) {
        if (cipherText == null) return null;
        try {
            byte[] ivct = Base64.getDecoder().decode(cipherText);
            byte[] iv = java.util.Arrays.copyOfRange(ivct, 0, 12);
            byte[] ct = java.util.Arrays.copyOfRange(ivct, 12, ivct.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(key), new GCMParameterSpec(128, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
