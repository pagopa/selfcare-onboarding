package it.pagopa.selfcare.onboarding.entity;

import lombok.Data;

@Data
public class Payment {
    private String holder;
    private String iban;
}