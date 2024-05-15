package it.pagopa.selfcare.onboarding.controller.response;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.controller.request.DataProtectionOfficerRequest;
import it.pagopa.selfcare.onboarding.controller.request.GeographicTaxonomyDto;
import it.pagopa.selfcare.onboarding.controller.request.PaymentServiceProviderRequest;
import it.pagopa.selfcare.onboarding.util.InstitutionPaSubunitType;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionResponse {

    private String id;
    private InstitutionType institutionType;
    private String taxCode;
    private String taxCodeInvoicing;
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

    private List<GeographicTaxonomyDto> geographicTaxonomies;

    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;

    private String supportEmail;
    private String supportPhone;

    private PaymentServiceProviderRequest paymentServiceProvider;
    private DataProtectionOfficerRequest dataProtectionOfficer;
}
