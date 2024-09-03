package it.pagopa.selfcare.onboarding.model;

import java.util.ArrayList;
import java.util.List;

public class AggregatesCsvResponse {
    private List<Csv> csvAggregateList;
    private List<Csv> validAggregates = new ArrayList<>();
    private List<RowError> rowErrorList;

    public AggregatesCsvResponse(List<Csv> csvAggregateList, List<RowError> rowErrorList) {
        this.csvAggregateList = csvAggregateList;
        this.rowErrorList = rowErrorList;
    }

    public List<Csv> getCsvAggregateList() {
        return csvAggregateList;
    }

    public void setCsvAggregateList(List<Csv> Csv) {
        this.csvAggregateList = csvAggregateList;
    }

    public List<Csv> getValidAggregates() {
        return validAggregates;
    }

    public void setValidAggregates(List<Csv> Csv) {
        this.validAggregates = validAggregates;
    }

    public List<RowError> getRowErrorList() {
        return rowErrorList;
    }

    public void setRowErrorList(List<RowError> rowErrorList) {
        this.rowErrorList = rowErrorList;
    }

}
