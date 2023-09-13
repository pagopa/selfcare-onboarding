package it.pagopa.selfcare.entity;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class Billing {
    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;
}
