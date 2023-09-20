package it.pagopa.selfcare.controller.response;

import it.pagopa.selfcare.controller.request.DataProtectionOfficerRequest;
import it.pagopa.selfcare.controller.request.PaymentServiceProviderRequest;
import it.pagopa.selfcare.util.InstitutionPaSubunitType;
import it.pagopa.selfcare.util.InstitutionType;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionResponse {


    private InstitutionType institutionType;
    private String taxCode;
    private String subunitCode;
    private InstitutionPaSubunitType subunitType;

    private String description;
    private String digitalAddress;
    private String address;
    private String zipCode;

    private List<String> geographicTaxonomyCodes;

    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;

    private String supportEmail;
    private String supportPhone;

    private PaymentServiceProviderRequest paymentServiceProvider;
    private DataProtectionOfficerRequest dataProtectionOfficer;
}
