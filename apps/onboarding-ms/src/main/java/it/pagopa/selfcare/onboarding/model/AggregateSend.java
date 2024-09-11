package it.pagopa.selfcare.onboarding.model;

import lombok.Data;

import java.util.List;

@Data
public class AggregateSend {
    private String description;
    private String pec;
    private String taxCode;
    private String vatNumber;
    private String codeSDI;
    private String address;
    private String city;
    private String province;
    private String subunitType;
    private String subunitCode;
    private List<AggregateUser> users;
}