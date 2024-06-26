package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.Origin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AggregateInstitution {

    private String taxCode;
    private String description;
    private String subunitCode;
    private String subunitType;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private String address;
    private String zipCode;
    private String originId;
    private Origin origin;

}
