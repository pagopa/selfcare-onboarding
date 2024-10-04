package it.pagopa.selfcare.onboarding.model;

import lombok.Data;
import wiremock.com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Data
public class Aggregate {
    private String subunitCode;
    private String subunitType;
    private String description;
    private String codeSDI;
    private List<AggregateUser> users;
    private String digitalAddress;
    private String taxCode;
    private String vatNumber;
    private String address;
    private String city;
    private String county;
    private String zipCode;
    private String originId;
    private String origin;
    private String taxCodePT;
    private String iban;
    private String service;
    private String syncAsyncMode;

    @JsonIgnoreProperties
    private Integer rowNumber;
}
