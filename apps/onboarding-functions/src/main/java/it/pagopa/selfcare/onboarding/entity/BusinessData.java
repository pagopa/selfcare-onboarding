package it.pagopa.selfcare.onboarding.entity;

public class BusinessData {

    private String businessRegisterNumber;
    private String legalRegisterNumber;
    private String legalRegisterName;

    public String getBusinessRegisterNumber() {
        return businessRegisterNumber;
    }

    public void setBusinessRegisterNumber(String businessRegisterNumber) {
        this.businessRegisterNumber = businessRegisterNumber;
    }

    public String getLegalRegisterNumber() {
        return legalRegisterNumber;
    }

    public void setLegalRegisterNumber(String legalRegisterNumber) {
        this.legalRegisterNumber = legalRegisterNumber;
    }

    public String getLegalRegisterName() {
        return legalRegisterName;
    }

    public void setLegalRegisterName(String legalRegisterName) {
        this.legalRegisterName = legalRegisterName;
    }
}
