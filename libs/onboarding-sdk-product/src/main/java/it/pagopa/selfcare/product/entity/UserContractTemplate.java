package it.pagopa.selfcare.product.entity;

import java.time.Instant;

public class UserContractTemplate {

    private Instant userContractTemplateUpdatedAt;
    private String userContractTemplatePath;
    private String userContractTemplateVersion;

    public Instant getUserContractTemplateUpdatedAt() {
        return userContractTemplateUpdatedAt;
    }

    public void setUserContractTemplateUpdatedAt(Instant userContractTemplateUpdatedAt) {
        this.userContractTemplateUpdatedAt = userContractTemplateUpdatedAt;
    }

    public String getUserContractTemplatePath() {
        return userContractTemplatePath;
    }

    public void setUserContractTemplatePath(String userContractTemplatePath) {
        this.userContractTemplatePath = userContractTemplatePath;
    }

    public String getUserContractTemplateVersion() {
        return userContractTemplateVersion;
    }

    public void setUserContractTemplateVersion(String userContractTemplateVersion) {
        this.userContractTemplateVersion = userContractTemplateVersion;
    }
}
