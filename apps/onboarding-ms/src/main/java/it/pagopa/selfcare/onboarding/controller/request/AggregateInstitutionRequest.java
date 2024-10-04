package it.pagopa.selfcare.onboarding.controller.request;

import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.entity.GeographicTaxonomy;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AggregateInstitutionRequest {
    @NotNull(message = "taxCode is required")
    private String taxCode;

    @NotNull(message = "description is required")
    private String description;

    private String subunitCode;
    private String subunitType;
    private String vatNumber;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private String address;
    private String zipCode;
    private String originId;
    private Origin origin;
    private List<UserRequest> users;
}
