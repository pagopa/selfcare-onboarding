package it.pagopa.selfcare.onboarding.entity;


import java.util.List;

public class PaymentServiceProvider {
    private String abiCode;
    private String businessRegisterNumber;
    private String legalRegisterNumber;
    private String legalRegisterName;
    private boolean vatNumberGroup;
    private List<String> providerNames;
    private String contractType;
    private String contractId;

    public String getAbiCode() {
        return abiCode;
    }

    public void setAbiCode(String abiCode) {
        this.abiCode = abiCode;
    }

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

    public boolean isVatNumberGroup() {
        return vatNumberGroup;
    }

    public void setVatNumberGroup(boolean vatNumberGroup) {
        this.vatNumberGroup = vatNumberGroup;
    }

    public List<String> getProviderNames() {
        return providerNames;
    }

    public void setProviderNames(List<String> providerNames) {
        this.providerNames = providerNames;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }
}
