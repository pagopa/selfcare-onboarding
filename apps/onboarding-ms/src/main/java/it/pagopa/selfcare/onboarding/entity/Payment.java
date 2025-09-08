package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import jakarta.validation.constraints.Pattern;
import java.util.Optional;
import lombok.Data;

@Data
public class Payment {

    @Pattern(regexp = "^IT\\d{2}[A-Z]\\d{5}\\d{5}[0-9A-Z]{12}$",
            message = "IBAN is not in an Italian format or is not 27 characters long")
    private String iban;

    private String holder;

    public String retrieveEncryptedHolder() {
        return Optional.ofNullable(holder)
                .map(DataEncryptionUtils::decrypt)
                .orElse("");
    }

    public void setHolder(String holder) {
        this.holder = Optional.ofNullable(holder)
                .map(DataEncryptionUtils::encrypt)
                .orElse("");
    }

    public String retrieveEncryptedIban() {
        return Optional.ofNullable(iban)
                .map(DataEncryptionUtils::decrypt)
                .orElse("");
    }

    public void setIban(String iban) {
        this.iban = Optional.ofNullable(iban)
                .map(DataEncryptionUtils::encrypt)
                .orElse("");
    }

}
