package it.pagopa.selfcare.onboarding.model;

import java.util.ArrayList;
import java.util.List;

public class AggregatesCsvResponse {
    private List<CsvAggregate> csvAggregateList;
    private List<CsvAggregate> validAggregates = new ArrayList<>();
    private List<RowError> rowErrorList;

    public AggregatesCsvResponse(List<CsvAggregate> csvAggregateList, List<RowError> rowErrorList) {
        this.csvAggregateList = csvAggregateList;
        this.rowErrorList = rowErrorList;
    }

    public List<CsvAggregate> getCsvAggregateList() {
        return csvAggregateList;
    }

    public void setCsvAggregateList(List<CsvAggregate> csvAggregateList) {
        this.csvAggregateList = csvAggregateList;
    }

    public List<CsvAggregate> getValidAggregates() {
        return validAggregates;
    }

    public void setValidAggregates(List<CsvAggregate> validAggregates) {
        this.validAggregates = validAggregates;
    }

    public List<RowError> getRowErrorList() {
        return rowErrorList;
    }

    public void setRowErrorList(List<RowError> rowErrorList) {
        this.rowErrorList = rowErrorList;
    }

}
