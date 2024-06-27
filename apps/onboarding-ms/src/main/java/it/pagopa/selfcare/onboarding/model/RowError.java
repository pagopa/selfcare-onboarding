package it.pagopa.selfcare.onboarding.model;

import jakarta.validation.constraints.NotNull;

public class RowError {
    @NotNull
    private Integer row;
    @NotNull
    private String cf;
    private String reason;

    public RowError(Integer row, String cf, String reason) {
        this.row = row;
        this.cf = cf;
        this.reason = reason;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public String getCf() {
        return cf;
    }

    public void setCf(String cf) {
        this.cf = cf;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
