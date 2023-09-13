package it.pagopa.selfcare.controller.request;

import it.pagopa.selfcare.util.InstitutionPaSubunitType;
import it.pagopa.selfcare.util.InstitutionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionBaseRequest {

    @NotNull(message = "institutionType is required")
    private InstitutionType institutionType;
    @NotEmpty(message = "taxCode is required")
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

    /* when onboarding is imported (es. from IO)*/
    private boolean imported;
}
