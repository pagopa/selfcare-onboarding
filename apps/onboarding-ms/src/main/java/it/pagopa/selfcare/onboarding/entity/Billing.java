package it.pagopa.selfcare.onboarding.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Billing {
    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;
    private String taxCodeInvoicing;
}
