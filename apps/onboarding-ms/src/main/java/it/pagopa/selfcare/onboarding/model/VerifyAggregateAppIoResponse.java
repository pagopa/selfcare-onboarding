package it.pagopa.selfcare.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor

public class VerifyAggregateAppIoResponse {
    private List<AggregateAppIo> aggregates;
    private List<RowError> errors;
}
