package it.pagopa.selfcare.onboarding.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Payment {

    private String holder;
    private String iban;

}