package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import lombok.Builder;

@Builder
public class Payment {

    private String holder;
    private String iban;

    public String getHolder() {
        return holder != null ? DataEncryptionUtils.decrypt(holder) : null;
    }

    public void setHolder(String holder) {
        this.holder = holder != null ? DataEncryptionUtils.encrypt(holder) : null;
    }

    public String getIban() {
        return iban != null ? DataEncryptionUtils.decrypt(iban) : null;
    }

    public void setIban(String iban) {
        this.iban = iban != null ? DataEncryptionUtils.encrypt(iban) : null;
    }
}