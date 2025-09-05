package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import jakarta.validation.constraints.Pattern;

public class Payment {

    @Pattern(regexp = "^IT\\d{2}[A-Z]\\d{5}\\d{5}[0-9A-Z]{12}$",
            message = "IBAN is not in an Italian format or is not 27 characters long")
    private String iban;

    private String holder;

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
