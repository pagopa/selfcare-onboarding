package it.pagopa.selfcare.product.entity;

import java.time.Instant;

public class ContractStorage {

    private Instant contractTemplateUpdatedAt;
    private String contractTemplatePath;
    private String contractTemplateVersion;

    public Instant getContractTemplateUpdatedAt() {
        return contractTemplateUpdatedAt;
    }

    public void setContractTemplateUpdatedAt(Instant contractTemplateUpdatedAt) {
        this.contractTemplateUpdatedAt = contractTemplateUpdatedAt;
    }

    public String getContractTemplatePath() {
        return contractTemplatePath;
    }

    public void setContractTemplatePath(String contractTemplatePath) {
        this.contractTemplatePath = contractTemplatePath;
    }

    public String getContractTemplateVersion() {
        return contractTemplateVersion;
    }

    public void setContractTemplateVersion(String contractTemplateVersion) {
        this.contractTemplateVersion = contractTemplateVersion;
    }
}
