package it.pagopa.selfcare.onboarding.model;

import it.pagopa.selfcare.onboarding.entity.Institution;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyAggregateResponse {

    private List<Institution> aggregates;
    private List<RowError> errors;

}
