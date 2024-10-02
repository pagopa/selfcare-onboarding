package it.pagopa.selfcare.onboarding.model;

import lombok.Data;
import wiremock.com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
public class AggregateAppIo {
    private String subunitCode;
    private String subunitType;
    private String description;
    private String digitalAddress;
    private String taxCode;
    private String vatNumber;
    private String address;
    private String city;
    private String county;
    private String zipCode;
    private String originId;
    private String origin;

    @JsonIgnoreProperties
    private Integer rowNumber;
}
