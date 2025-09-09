package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;

import java.util.Optional;

import lombok.Data;

@Data
public class Payment {
    private String holder;
    private String iban;

    public void setHolder(String holder) {
        this.holder = Optional.ofNullable(holder)
                .map(DataEncryptionUtils::encrypt)
                .orElse("");
    }

    public void setIban(String iban) {
        this.iban = Optional.ofNullable(iban)
                .map(DataEncryptionUtils::encrypt)
                .orElse("");
    }

    public String retrieveEncryptedHolder() {
        return Optional.ofNullable(holder)
                .map(DataEncryptionUtils::decrypt)
                .orElse("");
    }

    public String retrieveEncryptedIban() {
        return Optional.ofNullable(iban)
                .map(DataEncryptionUtils::decrypt)
                .orElse("");
    }

}
