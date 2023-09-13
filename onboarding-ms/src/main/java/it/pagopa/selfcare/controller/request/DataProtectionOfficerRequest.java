package it.pagopa.selfcare.controller.request;

import lombok.Data;

@Data
public class DataProtectionOfficerRequest {
    private String address;
    private String email;
    private String pec;
}
