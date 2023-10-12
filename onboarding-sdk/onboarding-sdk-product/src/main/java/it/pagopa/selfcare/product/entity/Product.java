package it.pagopa.selfcare.product.entity;


import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.commons.base.utils.InstitutionType;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

public class Product {

    private String id;
    private String logo;
    private String depictImageUrl;
    private String title;
    private String logoBgColor;
    private String description;
    private String urlPublic;
    private String urlBO;
    private Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;
    private EnumMap<PartyRole, ProductRoleInfo> roleMappings;
    private String roleManagementURL;
    private Instant contractTemplateUpdatedAt;
    private String contractTemplatePath;
    private String contractTemplateVersion;
    private Map<InstitutionType, ContractStorage> institutionContractMappings;
    private boolean enabled = true;
    private boolean delegable;
    private ProductStatus status;
    private String parentId;
    private String identityTokenAudience;
    private Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations;
    private Product parent;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getDepictImageUrl() {
        return depictImageUrl;
    }

    public void setDepictImageUrl(String depictImageUrl) {
        this.depictImageUrl = depictImageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLogoBgColor() {
        return logoBgColor;
    }

    public void setLogoBgColor(String logoBgColor) {
        this.logoBgColor = logoBgColor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrlPublic() {
        return urlPublic;
    }

    public void setUrlPublic(String urlPublic) {
        this.urlPublic = urlPublic;
    }

    public String getUrlBO() {
        return urlBO;
    }

    public void setUrlBO(String urlBO) {
        this.urlBO = urlBO;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Instant modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public EnumMap<PartyRole, ProductRoleInfo> getRoleMappings() {
        return roleMappings;
    }

    public void setRoleMappings(EnumMap<PartyRole, ProductRoleInfo> roleMappings) {
        this.roleMappings = roleMappings;
    }

    public String getRoleManagementURL() {
        return roleManagementURL;
    }

    public void setRoleManagementURL(String roleManagementURL) {
        this.roleManagementURL = roleManagementURL;
    }

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

    public Map<InstitutionType, ContractStorage> getInstitutionContractMappings() {
        return institutionContractMappings;
    }

    public void setInstitutionContractMappings(Map<InstitutionType, ContractStorage> institutionContractMappings) {
        this.institutionContractMappings = institutionContractMappings;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDelegable() {
        return delegable;
    }

    public void setDelegable(boolean delegable) {
        this.delegable = delegable;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getIdentityTokenAudience() {
        return identityTokenAudience;
    }

    public void setIdentityTokenAudience(String identityTokenAudience) {
        this.identityTokenAudience = identityTokenAudience;
    }

    public Map<String, BackOfficeConfigurations> getBackOfficeEnvironmentConfigurations() {
        return backOfficeEnvironmentConfigurations;
    }

    public void setBackOfficeEnvironmentConfigurations(Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations) {
        this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
    }

    public Product getParent() {
        return parent;
    }

    public void setParent(Product parent) {
        this.parent = parent;
    }
}
