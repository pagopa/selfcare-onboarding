package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.util.InstitutionPaSubunitType;
import lombok.Data;

import java.util.List;

@Data
public class Institution {

    private String id;
    private InstitutionType institutionType;
    private String taxCode;
    private String subunitCode;
    private InstitutionPaSubunitType subunitType;
    private Origin origin;
    private String city;
    private String country;
    private String county;
    private String description;
    private String digitalAddress;
    private String address;
    private String zipCode;

    private List<GeographicTaxonomy> geographicTaxonomies;

    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;

    private String supportEmail;
    private String supportPhone;

    private boolean imported;

    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;

    private String parentDescription;
}
