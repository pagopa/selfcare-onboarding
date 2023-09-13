package it.pagopa.selfcare.controller.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.util.InstitutionType;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnboardingResponse {

    private String id;
    private String externalId;
    private String originId;
    private String description;
    private String origin;
    private InstitutionType institutionType;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String taxCode;
    private String pricingPlan;
    //private Billing billing;
    //private List<GeoTaxonomies> geographicTaxonomies;
    //private List<AttributesResponse> attributes;
    //private String state;
    //private PartyRole role;
    private ProductInfo productInfo;
    private BusinessData businessData;
    private SupportContact supportContact;
    private PaymentServiceProviderRequest paymentServiceProvider;
    private DataProtectionOfficerRequest dataProtectionOfficer;
    private String parentDescription;
    private String rootParentId;
    private String subunitCode;
    private String subunitType;
    private String aooParentCode;

}
