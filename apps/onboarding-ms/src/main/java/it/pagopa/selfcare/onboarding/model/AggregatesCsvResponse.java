package it.pagopa.selfcare.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class AggregatesCsvResponse {
    private List<CsvAggregate> csvAggregateList;
    private List<CsvAggregate> validAggregates = new ArrayList<>();
    private List<RowError> rowErrorList;

    public AggregatesCsvResponse(List<CsvAggregate> csvAggregateList, List<RowError> rowErrorList) {
        this.csvAggregateList = csvAggregateList;
        this.rowErrorList = rowErrorList;
    }

    public AggregatesCsvResponse() {
    }
}
