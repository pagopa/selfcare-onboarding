package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.OffsetDateTime;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ProductOperations  {

    private Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations;
    private String contractTemplatePath;
    private OffsetDateTime contractTemplateUpdatedAt;
    private String contractTemplateVersion;
    private OffsetDateTime createdAt;
    private String createdBy;
    private Boolean delegable;
    private String depictImageUrl;
    private String description;
    private Boolean enabled;
    private String id;
    private String identityTokenAudience;
    private Map<String, ContractOperations> institutionContractMappings;
    private String logo;
    private String logoBgColor;
    private OffsetDateTime modifiedAt;
    private String modifiedBy;
    private String parentId;
    private ProductOperations productOperations;
    private String roleManagementURL;
    private Map<String, ProductRoleInfoOperations> roleMappings;

    public enum StatusEnum {
        ACTIVE(String.valueOf("ACTIVE")), INACTIVE(String.valueOf("INACTIVE")), PHASE_OUT(String.valueOf("PHASE_OUT")), TESTING(String.valueOf("TESTING"));

        // caching enum access
        private static final java.util.EnumSet<StatusEnum> values = java.util.EnumSet.allOf(StatusEnum.class);

        String value;

        StatusEnum (String v) {
            value = v;
        }

        @JsonValue
        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static StatusEnum fromValue(String v) {
            for (StatusEnum b : values) {
                if (String.valueOf(b.value).equalsIgnoreCase(v)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + v + "'");
        }
    }
    private StatusEnum status;
    private String title;
    private String urlBO;
    private String urlPublic;

    /**
    * Get backOfficeEnvironmentConfigurations
    * @return backOfficeEnvironmentConfigurations
    **/
    @JsonProperty("backOfficeEnvironmentConfigurations")
    public Map<String, BackOfficeConfigurations> getBackOfficeEnvironmentConfigurations() {
        return backOfficeEnvironmentConfigurations;
    }

    /**
     * Set backOfficeEnvironmentConfigurations
     **/
    public void setBackOfficeEnvironmentConfigurations(Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations) {
        this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
    }

    public ProductOperations backOfficeEnvironmentConfigurations(Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations) {
        this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
        return this;
    }
    public ProductOperations putBackOfficeEnvironmentConfigurationsItem(String key, BackOfficeConfigurations backOfficeEnvironmentConfigurationsItem) {
        this.backOfficeEnvironmentConfigurations.put(key, backOfficeEnvironmentConfigurationsItem);
        return this;
    }

    /**
    * Get contractTemplatePath
    * @return contractTemplatePath
    **/
    @JsonProperty("contractTemplatePath")
    public String getContractTemplatePath() {
        return contractTemplatePath;
    }

    /**
     * Set contractTemplatePath
     **/
    public void setContractTemplatePath(String contractTemplatePath) {
        this.contractTemplatePath = contractTemplatePath;
    }

    public ProductOperations contractTemplatePath(String contractTemplatePath) {
        this.contractTemplatePath = contractTemplatePath;
        return this;
    }

    /**
    * Get contractTemplateUpdatedAt
    * @return contractTemplateUpdatedAt
    **/
    @JsonProperty("contractTemplateUpdatedAt")
    public OffsetDateTime getContractTemplateUpdatedAt() {
        return contractTemplateUpdatedAt;
    }

    /**
     * Set contractTemplateUpdatedAt
     **/
    public void setContractTemplateUpdatedAt(OffsetDateTime contractTemplateUpdatedAt) {
        this.contractTemplateUpdatedAt = contractTemplateUpdatedAt;
    }

    public ProductOperations contractTemplateUpdatedAt(OffsetDateTime contractTemplateUpdatedAt) {
        this.contractTemplateUpdatedAt = contractTemplateUpdatedAt;
        return this;
    }

    /**
    * Get contractTemplateVersion
    * @return contractTemplateVersion
    **/
    @JsonProperty("contractTemplateVersion")
    public String getContractTemplateVersion() {
        return contractTemplateVersion;
    }

    /**
     * Set contractTemplateVersion
     **/
    public void setContractTemplateVersion(String contractTemplateVersion) {
        this.contractTemplateVersion = contractTemplateVersion;
    }

    public ProductOperations contractTemplateVersion(String contractTemplateVersion) {
        this.contractTemplateVersion = contractTemplateVersion;
        return this;
    }

    /**
    * Get createdAt
    * @return createdAt
    **/
    @JsonProperty("createdAt")
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Set createdAt
     **/
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ProductOperations createdAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
    * Get createdBy
    * @return createdBy
    **/
    @JsonProperty("createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Set createdBy
     **/
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public ProductOperations createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    /**
    * Get delegable
    * @return delegable
    **/
    @JsonProperty("delegable")
    public Boolean getDelegable() {
        return delegable;
    }

    /**
     * Set delegable
     **/
    public void setDelegable(Boolean delegable) {
        this.delegable = delegable;
    }

    public ProductOperations delegable(Boolean delegable) {
        this.delegable = delegable;
        return this;
    }

    /**
    * Get depictImageUrl
    * @return depictImageUrl
    **/
    @JsonProperty("depictImageUrl")
    public String getDepictImageUrl() {
        return depictImageUrl;
    }

    /**
     * Set depictImageUrl
     **/
    public void setDepictImageUrl(String depictImageUrl) {
        this.depictImageUrl = depictImageUrl;
    }

    public ProductOperations depictImageUrl(String depictImageUrl) {
        this.depictImageUrl = depictImageUrl;
        return this;
    }

    /**
    * Get description
    * @return description
    **/
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    /**
     * Set description
     **/
    public void setDescription(String description) {
        this.description = description;
    }

    public ProductOperations description(String description) {
        this.description = description;
        return this;
    }

    /**
    * Get enabled
    * @return enabled
    **/
    @JsonProperty("enabled")
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Set enabled
     **/
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public ProductOperations enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
    * Get id
    * @return id
    **/
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     * Set id
     **/
    public void setId(String id) {
        this.id = id;
    }

    public ProductOperations id(String id) {
        this.id = id;
        return this;
    }

    /**
    * Get identityTokenAudience
    * @return identityTokenAudience
    **/
    @JsonProperty("identityTokenAudience")
    public String getIdentityTokenAudience() {
        return identityTokenAudience;
    }

    /**
     * Set identityTokenAudience
     **/
    public void setIdentityTokenAudience(String identityTokenAudience) {
        this.identityTokenAudience = identityTokenAudience;
    }

    public ProductOperations identityTokenAudience(String identityTokenAudience) {
        this.identityTokenAudience = identityTokenAudience;
        return this;
    }

    /**
    * Get institutionContractMappings
    * @return institutionContractMappings
    **/
    @JsonProperty("institutionContractMappings")
    public Map<String, ContractOperations> getInstitutionContractMappings() {
        return institutionContractMappings;
    }

    /**
     * Set institutionContractMappings
     **/
    public void setInstitutionContractMappings(Map<String, ContractOperations> institutionContractMappings) {
        this.institutionContractMappings = institutionContractMappings;
    }

    public ProductOperations institutionContractMappings(Map<String, ContractOperations> institutionContractMappings) {
        this.institutionContractMappings = institutionContractMappings;
        return this;
    }
    public ProductOperations putInstitutionContractMappingsItem(String key, ContractOperations institutionContractMappingsItem) {
        this.institutionContractMappings.put(key, institutionContractMappingsItem);
        return this;
    }

    /**
    * Get logo
    * @return logo
    **/
    @JsonProperty("logo")
    public String getLogo() {
        return logo;
    }

    /**
     * Set logo
     **/
    public void setLogo(String logo) {
        this.logo = logo;
    }

    public ProductOperations logo(String logo) {
        this.logo = logo;
        return this;
    }

    /**
    * Get logoBgColor
    * @return logoBgColor
    **/
    @JsonProperty("logoBgColor")
    public String getLogoBgColor() {
        return logoBgColor;
    }

    /**
     * Set logoBgColor
     **/
    public void setLogoBgColor(String logoBgColor) {
        this.logoBgColor = logoBgColor;
    }

    public ProductOperations logoBgColor(String logoBgColor) {
        this.logoBgColor = logoBgColor;
        return this;
    }

    /**
    * Get modifiedAt
    * @return modifiedAt
    **/
    @JsonProperty("modifiedAt")
    public OffsetDateTime getModifiedAt() {
        return modifiedAt;
    }

    /**
     * Set modifiedAt
     **/
    public void setModifiedAt(OffsetDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public ProductOperations modifiedAt(OffsetDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
        return this;
    }

    /**
    * Get modifiedBy
    * @return modifiedBy
    **/
    @JsonProperty("modifiedBy")
    public String getModifiedBy() {
        return modifiedBy;
    }

    /**
     * Set modifiedBy
     **/
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public ProductOperations modifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
        return this;
    }

    /**
    * Get parentId
    * @return parentId
    **/
    @JsonProperty("parentId")
    public String getParentId() {
        return parentId;
    }

    /**
     * Set parentId
     **/
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public ProductOperations parentId(String parentId) {
        this.parentId = parentId;
        return this;
    }

    /**
    * Get productOperations
    * @return productOperations
    **/
    @JsonProperty("productOperations")
    public ProductOperations getProductOperations() {
        return productOperations;
    }

    /**
     * Set productOperations
     **/
    public void setProductOperations(ProductOperations productOperations) {
        this.productOperations = productOperations;
    }

    public ProductOperations productOperations(ProductOperations productOperations) {
        this.productOperations = productOperations;
        return this;
    }

    /**
    * Get roleManagementURL
    * @return roleManagementURL
    **/
    @JsonProperty("roleManagementURL")
    public String getRoleManagementURL() {
        return roleManagementURL;
    }

    /**
     * Set roleManagementURL
     **/
    public void setRoleManagementURL(String roleManagementURL) {
        this.roleManagementURL = roleManagementURL;
    }

    public ProductOperations roleManagementURL(String roleManagementURL) {
        this.roleManagementURL = roleManagementURL;
        return this;
    }

    /**
    * Get roleMappings
    * @return roleMappings
    **/
    @JsonProperty("roleMappings")
    public Map<String, ProductRoleInfoOperations> getRoleMappings() {
        return roleMappings;
    }

    /**
     * Set roleMappings
     **/
    public void setRoleMappings(Map<String, ProductRoleInfoOperations> roleMappings) {
        this.roleMappings = roleMappings;
    }

    public ProductOperations roleMappings(Map<String, ProductRoleInfoOperations> roleMappings) {
        this.roleMappings = roleMappings;
        return this;
    }
    public ProductOperations putRoleMappingsItem(String key, ProductRoleInfoOperations roleMappingsItem) {
        this.roleMappings.put(key, roleMappingsItem);
        return this;
    }

    /**
    * Get status
    * @return status
    **/
    @JsonProperty("status")
    public StatusEnum getStatus() {
        return status;
    }

    /**
     * Set status
     **/
    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public ProductOperations status(StatusEnum status) {
        this.status = status;
        return this;
    }

    /**
    * Get title
    * @return title
    **/
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    /**
     * Set title
     **/
    public void setTitle(String title) {
        this.title = title;
    }

    public ProductOperations title(String title) {
        this.title = title;
        return this;
    }

    /**
    * Get urlBO
    * @return urlBO
    **/
    @JsonProperty("urlBO")
    public String getUrlBO() {
        return urlBO;
    }

    /**
     * Set urlBO
     **/
    public void setUrlBO(String urlBO) {
        this.urlBO = urlBO;
    }

    public ProductOperations urlBO(String urlBO) {
        this.urlBO = urlBO;
        return this;
    }

    /**
    * Get urlPublic
    * @return urlPublic
    **/
    @JsonProperty("urlPublic")
    public String getUrlPublic() {
        return urlPublic;
    }

    /**
     * Set urlPublic
     **/
    public void setUrlPublic(String urlPublic) {
        this.urlPublic = urlPublic;
    }

    public ProductOperations urlPublic(String urlPublic) {
        this.urlPublic = urlPublic;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ProductOperations {\n");

        sb.append("    backOfficeEnvironmentConfigurations: ").append(toIndentedString(backOfficeEnvironmentConfigurations)).append("\n");
        sb.append("    contractTemplatePath: ").append(toIndentedString(contractTemplatePath)).append("\n");
        sb.append("    contractTemplateUpdatedAt: ").append(toIndentedString(contractTemplateUpdatedAt)).append("\n");
        sb.append("    contractTemplateVersion: ").append(toIndentedString(contractTemplateVersion)).append("\n");
        sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
        sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
        sb.append("    delegable: ").append(toIndentedString(delegable)).append("\n");
        sb.append("    depictImageUrl: ").append(toIndentedString(depictImageUrl)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    identityTokenAudience: ").append(toIndentedString(identityTokenAudience)).append("\n");
        sb.append("    institutionContractMappings: ").append(toIndentedString(institutionContractMappings)).append("\n");
        sb.append("    logo: ").append(toIndentedString(logo)).append("\n");
        sb.append("    logoBgColor: ").append(toIndentedString(logoBgColor)).append("\n");
        sb.append("    modifiedAt: ").append(toIndentedString(modifiedAt)).append("\n");
        sb.append("    modifiedBy: ").append(toIndentedString(modifiedBy)).append("\n");
        sb.append("    parentId: ").append(toIndentedString(parentId)).append("\n");
        sb.append("    productOperations: ").append(toIndentedString(productOperations)).append("\n");
        sb.append("    roleManagementURL: ").append(toIndentedString(roleManagementURL)).append("\n");
        sb.append("    roleMappings: ").append(toIndentedString(roleMappings)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    urlBO: ").append(toIndentedString(urlBO)).append("\n");
        sb.append("    urlPublic: ").append(toIndentedString(urlPublic)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private static String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
    public static class ProductOperationsQueryParam  {

        @jakarta.ws.rs.QueryParam("backOfficeEnvironmentConfigurations")
        private Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations = null;
        @jakarta.ws.rs.QueryParam("contractTemplatePath")
        private String contractTemplatePath;
        @jakarta.ws.rs.QueryParam("contractTemplateUpdatedAt")
        private OffsetDateTime contractTemplateUpdatedAt;
        @jakarta.ws.rs.QueryParam("contractTemplateVersion")
        private String contractTemplateVersion;
        @jakarta.ws.rs.QueryParam("createdAt")
        private OffsetDateTime createdAt;
        @jakarta.ws.rs.QueryParam("createdBy")
        private String createdBy;
        @jakarta.ws.rs.QueryParam("delegable")
        private Boolean delegable;
        @jakarta.ws.rs.QueryParam("depictImageUrl")
        private String depictImageUrl;
        @jakarta.ws.rs.QueryParam("description")
        private String description;
        @jakarta.ws.rs.QueryParam("enabled")
        private Boolean enabled;
        @jakarta.ws.rs.QueryParam("id")
        private String id;
        @jakarta.ws.rs.QueryParam("identityTokenAudience")
        private String identityTokenAudience;
        @jakarta.ws.rs.QueryParam("institutionContractMappings")
        private Map<String, ContractOperations> institutionContractMappings = null;
        @jakarta.ws.rs.QueryParam("logo")
        private String logo;
        @jakarta.ws.rs.QueryParam("logoBgColor")
        private String logoBgColor;
        @jakarta.ws.rs.QueryParam("modifiedAt")
        private OffsetDateTime modifiedAt;
        @jakarta.ws.rs.QueryParam("modifiedBy")
        private String modifiedBy;
        @jakarta.ws.rs.QueryParam("parentId")
        private String parentId;
        @jakarta.ws.rs.QueryParam("productOperations")
        private ProductOperations productOperations;
        @jakarta.ws.rs.QueryParam("roleManagementURL")
        private String roleManagementURL;
        @jakarta.ws.rs.QueryParam("roleMappings")
        private Map<String, ProductRoleInfoOperations> roleMappings = null;

    public enum StatusEnum {
        ACTIVE(String.valueOf("ACTIVE")), INACTIVE(String.valueOf("INACTIVE")), PHASE_OUT(String.valueOf("PHASE_OUT")), TESTING(String.valueOf("TESTING"));

        // caching enum access
        private static final java.util.EnumSet<StatusEnum> values = java.util.EnumSet.allOf(StatusEnum.class);

        String value;

        StatusEnum (String v) {
            value = v;
        }

        @JsonValue
        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static StatusEnum fromValue(String v) {
            for (StatusEnum b : values) {
                if (String.valueOf(b.value).equalsIgnoreCase(v)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + v + "'");
        }
    }
        private StatusEnum status;
        @jakarta.ws.rs.QueryParam("title")
        private String title;
        @jakarta.ws.rs.QueryParam("urlBO")
        private String urlBO;
        @jakarta.ws.rs.QueryParam("urlPublic")
        private String urlPublic;

        /**
        * Get backOfficeEnvironmentConfigurations
        * @return backOfficeEnvironmentConfigurations
        **/
        @JsonProperty("backOfficeEnvironmentConfigurations")
        public Map<String, BackOfficeConfigurations> getBackOfficeEnvironmentConfigurations() {
            return backOfficeEnvironmentConfigurations;
        }

        /**
         * Set backOfficeEnvironmentConfigurations
         **/
        public void setBackOfficeEnvironmentConfigurations(Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations) {
            this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
        }

        public ProductOperationsQueryParam backOfficeEnvironmentConfigurations(Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations) {
            this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
            return this;
        }
        public ProductOperationsQueryParam putBackOfficeEnvironmentConfigurationsItem(String key, BackOfficeConfigurations backOfficeEnvironmentConfigurationsItem) {
            this.backOfficeEnvironmentConfigurations.put(key, backOfficeEnvironmentConfigurationsItem);
            return this;
        }

        /**
        * Get contractTemplatePath
        * @return contractTemplatePath
        **/
        @JsonProperty("contractTemplatePath")
        public String getContractTemplatePath() {
            return contractTemplatePath;
        }

        /**
         * Set contractTemplatePath
         **/
        public void setContractTemplatePath(String contractTemplatePath) {
            this.contractTemplatePath = contractTemplatePath;
        }

        public ProductOperationsQueryParam contractTemplatePath(String contractTemplatePath) {
            this.contractTemplatePath = contractTemplatePath;
            return this;
        }

        /**
        * Get contractTemplateUpdatedAt
        * @return contractTemplateUpdatedAt
        **/
        @JsonProperty("contractTemplateUpdatedAt")
        public OffsetDateTime getContractTemplateUpdatedAt() {
            return contractTemplateUpdatedAt;
        }

        /**
         * Set contractTemplateUpdatedAt
         **/
        public void setContractTemplateUpdatedAt(OffsetDateTime contractTemplateUpdatedAt) {
            this.contractTemplateUpdatedAt = contractTemplateUpdatedAt;
        }

        public ProductOperationsQueryParam contractTemplateUpdatedAt(OffsetDateTime contractTemplateUpdatedAt) {
            this.contractTemplateUpdatedAt = contractTemplateUpdatedAt;
            return this;
        }

        /**
        * Get contractTemplateVersion
        * @return contractTemplateVersion
        **/
        @JsonProperty("contractTemplateVersion")
        public String getContractTemplateVersion() {
            return contractTemplateVersion;
        }

        /**
         * Set contractTemplateVersion
         **/
        public void setContractTemplateVersion(String contractTemplateVersion) {
            this.contractTemplateVersion = contractTemplateVersion;
        }

        public ProductOperationsQueryParam contractTemplateVersion(String contractTemplateVersion) {
            this.contractTemplateVersion = contractTemplateVersion;
            return this;
        }

        /**
        * Get createdAt
        * @return createdAt
        **/
        @JsonProperty("createdAt")
        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        /**
         * Set createdAt
         **/
        public void setCreatedAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public ProductOperationsQueryParam createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
        * Get createdBy
        * @return createdBy
        **/
        @JsonProperty("createdBy")
        public String getCreatedBy() {
            return createdBy;
        }

        /**
         * Set createdBy
         **/
        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public ProductOperationsQueryParam createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        /**
        * Get delegable
        * @return delegable
        **/
        @JsonProperty("delegable")
        public Boolean getDelegable() {
            return delegable;
        }

        /**
         * Set delegable
         **/
        public void setDelegable(Boolean delegable) {
            this.delegable = delegable;
        }

        public ProductOperationsQueryParam delegable(Boolean delegable) {
            this.delegable = delegable;
            return this;
        }

        /**
        * Get depictImageUrl
        * @return depictImageUrl
        **/
        @JsonProperty("depictImageUrl")
        public String getDepictImageUrl() {
            return depictImageUrl;
        }

        /**
         * Set depictImageUrl
         **/
        public void setDepictImageUrl(String depictImageUrl) {
            this.depictImageUrl = depictImageUrl;
        }

        public ProductOperationsQueryParam depictImageUrl(String depictImageUrl) {
            this.depictImageUrl = depictImageUrl;
            return this;
        }

        /**
        * Get description
        * @return description
        **/
        @JsonProperty("description")
        public String getDescription() {
            return description;
        }

        /**
         * Set description
         **/
        public void setDescription(String description) {
            this.description = description;
        }

        public ProductOperationsQueryParam description(String description) {
            this.description = description;
            return this;
        }

        /**
        * Get enabled
        * @return enabled
        **/
        @JsonProperty("enabled")
        public Boolean getEnabled() {
            return enabled;
        }

        /**
         * Set enabled
         **/
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public ProductOperationsQueryParam enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
        * Get id
        * @return id
        **/
        @JsonProperty("id")
        public String getId() {
            return id;
        }

        /**
         * Set id
         **/
        public void setId(String id) {
            this.id = id;
        }

        public ProductOperationsQueryParam id(String id) {
            this.id = id;
            return this;
        }

        /**
        * Get identityTokenAudience
        * @return identityTokenAudience
        **/
        @JsonProperty("identityTokenAudience")
        public String getIdentityTokenAudience() {
            return identityTokenAudience;
        }

        /**
         * Set identityTokenAudience
         **/
        public void setIdentityTokenAudience(String identityTokenAudience) {
            this.identityTokenAudience = identityTokenAudience;
        }

        public ProductOperationsQueryParam identityTokenAudience(String identityTokenAudience) {
            this.identityTokenAudience = identityTokenAudience;
            return this;
        }

        /**
        * Get institutionContractMappings
        * @return institutionContractMappings
        **/
        @JsonProperty("institutionContractMappings")
        public Map<String, ContractOperations> getInstitutionContractMappings() {
            return institutionContractMappings;
        }

        /**
         * Set institutionContractMappings
         **/
        public void setInstitutionContractMappings(Map<String, ContractOperations> institutionContractMappings) {
            this.institutionContractMappings = institutionContractMappings;
        }

        public ProductOperationsQueryParam institutionContractMappings(Map<String, ContractOperations> institutionContractMappings) {
            this.institutionContractMappings = institutionContractMappings;
            return this;
        }
        public ProductOperationsQueryParam putInstitutionContractMappingsItem(String key, ContractOperations institutionContractMappingsItem) {
            this.institutionContractMappings.put(key, institutionContractMappingsItem);
            return this;
        }

        /**
        * Get logo
        * @return logo
        **/
        @JsonProperty("logo")
        public String getLogo() {
            return logo;
        }

        /**
         * Set logo
         **/
        public void setLogo(String logo) {
            this.logo = logo;
        }

        public ProductOperationsQueryParam logo(String logo) {
            this.logo = logo;
            return this;
        }

        /**
        * Get logoBgColor
        * @return logoBgColor
        **/
        @JsonProperty("logoBgColor")
        public String getLogoBgColor() {
            return logoBgColor;
        }

        /**
         * Set logoBgColor
         **/
        public void setLogoBgColor(String logoBgColor) {
            this.logoBgColor = logoBgColor;
        }

        public ProductOperationsQueryParam logoBgColor(String logoBgColor) {
            this.logoBgColor = logoBgColor;
            return this;
        }

        /**
        * Get modifiedAt
        * @return modifiedAt
        **/
        @JsonProperty("modifiedAt")
        public OffsetDateTime getModifiedAt() {
            return modifiedAt;
        }

        /**
         * Set modifiedAt
         **/
        public void setModifiedAt(OffsetDateTime modifiedAt) {
            this.modifiedAt = modifiedAt;
        }

        public ProductOperationsQueryParam modifiedAt(OffsetDateTime modifiedAt) {
            this.modifiedAt = modifiedAt;
            return this;
        }

        /**
        * Get modifiedBy
        * @return modifiedBy
        **/
        @JsonProperty("modifiedBy")
        public String getModifiedBy() {
            return modifiedBy;
        }

        /**
         * Set modifiedBy
         **/
        public void setModifiedBy(String modifiedBy) {
            this.modifiedBy = modifiedBy;
        }

        public ProductOperationsQueryParam modifiedBy(String modifiedBy) {
            this.modifiedBy = modifiedBy;
            return this;
        }

        /**
        * Get parentId
        * @return parentId
        **/
        @JsonProperty("parentId")
        public String getParentId() {
            return parentId;
        }

        /**
         * Set parentId
         **/
        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public ProductOperationsQueryParam parentId(String parentId) {
            this.parentId = parentId;
            return this;
        }

        /**
        * Get productOperations
        * @return productOperations
        **/
        @JsonProperty("productOperations")
        public ProductOperations getProductOperations() {
            return productOperations;
        }

        /**
         * Set productOperations
         **/
        public void setProductOperations(ProductOperations productOperations) {
            this.productOperations = productOperations;
        }

        public ProductOperationsQueryParam productOperations(ProductOperations productOperations) {
            this.productOperations = productOperations;
            return this;
        }

        /**
        * Get roleManagementURL
        * @return roleManagementURL
        **/
        @JsonProperty("roleManagementURL")
        public String getRoleManagementURL() {
            return roleManagementURL;
        }

        /**
         * Set roleManagementURL
         **/
        public void setRoleManagementURL(String roleManagementURL) {
            this.roleManagementURL = roleManagementURL;
        }

        public ProductOperationsQueryParam roleManagementURL(String roleManagementURL) {
            this.roleManagementURL = roleManagementURL;
            return this;
        }

        /**
        * Get roleMappings
        * @return roleMappings
        **/
        @JsonProperty("roleMappings")
        public Map<String, ProductRoleInfoOperations> getRoleMappings() {
            return roleMappings;
        }

        /**
         * Set roleMappings
         **/
        public void setRoleMappings(Map<String, ProductRoleInfoOperations> roleMappings) {
            this.roleMappings = roleMappings;
        }

        public ProductOperationsQueryParam roleMappings(Map<String, ProductRoleInfoOperations> roleMappings) {
            this.roleMappings = roleMappings;
            return this;
        }
        public ProductOperationsQueryParam putRoleMappingsItem(String key, ProductRoleInfoOperations roleMappingsItem) {
            this.roleMappings.put(key, roleMappingsItem);
            return this;
        }

        /**
        * Get status
        * @return status
        **/
        @JsonProperty("status")
        public StatusEnum getStatus() {
            return status;
        }

        /**
         * Set status
         **/
        public void setStatus(StatusEnum status) {
            this.status = status;
        }

        public ProductOperationsQueryParam status(StatusEnum status) {
            this.status = status;
            return this;
        }

        /**
        * Get title
        * @return title
        **/
        @JsonProperty("title")
        public String getTitle() {
            return title;
        }

        /**
         * Set title
         **/
        public void setTitle(String title) {
            this.title = title;
        }

        public ProductOperationsQueryParam title(String title) {
            this.title = title;
            return this;
        }

        /**
        * Get urlBO
        * @return urlBO
        **/
        @JsonProperty("urlBO")
        public String getUrlBO() {
            return urlBO;
        }

        /**
         * Set urlBO
         **/
        public void setUrlBO(String urlBO) {
            this.urlBO = urlBO;
        }

        public ProductOperationsQueryParam urlBO(String urlBO) {
            this.urlBO = urlBO;
            return this;
        }

        /**
        * Get urlPublic
        * @return urlPublic
        **/
        @JsonProperty("urlPublic")
        public String getUrlPublic() {
            return urlPublic;
        }

        /**
         * Set urlPublic
         **/
        public void setUrlPublic(String urlPublic) {
            this.urlPublic = urlPublic;
        }

        public ProductOperationsQueryParam urlPublic(String urlPublic) {
            this.urlPublic = urlPublic;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class ProductOperationsQueryParam {\n");

            sb.append("    backOfficeEnvironmentConfigurations: ").append(toIndentedString(backOfficeEnvironmentConfigurations)).append("\n");
            sb.append("    contractTemplatePath: ").append(toIndentedString(contractTemplatePath)).append("\n");
            sb.append("    contractTemplateUpdatedAt: ").append(toIndentedString(contractTemplateUpdatedAt)).append("\n");
            sb.append("    contractTemplateVersion: ").append(toIndentedString(contractTemplateVersion)).append("\n");
            sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
            sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
            sb.append("    delegable: ").append(toIndentedString(delegable)).append("\n");
            sb.append("    depictImageUrl: ").append(toIndentedString(depictImageUrl)).append("\n");
            sb.append("    description: ").append(toIndentedString(description)).append("\n");
            sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
            sb.append("    id: ").append(toIndentedString(id)).append("\n");
            sb.append("    identityTokenAudience: ").append(toIndentedString(identityTokenAudience)).append("\n");
            sb.append("    institutionContractMappings: ").append(toIndentedString(institutionContractMappings)).append("\n");
            sb.append("    logo: ").append(toIndentedString(logo)).append("\n");
            sb.append("    logoBgColor: ").append(toIndentedString(logoBgColor)).append("\n");
            sb.append("    modifiedAt: ").append(toIndentedString(modifiedAt)).append("\n");
            sb.append("    modifiedBy: ").append(toIndentedString(modifiedBy)).append("\n");
            sb.append("    parentId: ").append(toIndentedString(parentId)).append("\n");
            sb.append("    productOperations: ").append(toIndentedString(productOperations)).append("\n");
            sb.append("    roleManagementURL: ").append(toIndentedString(roleManagementURL)).append("\n");
            sb.append("    roleMappings: ").append(toIndentedString(roleMappings)).append("\n");
            sb.append("    status: ").append(toIndentedString(status)).append("\n");
            sb.append("    title: ").append(toIndentedString(title)).append("\n");
            sb.append("    urlBO: ").append(toIndentedString(urlBO)).append("\n");
            sb.append("    urlPublic: ").append(toIndentedString(urlPublic)).append("\n");
            sb.append("}");
            return sb.toString();
        }

        /**
         * Convert the given object to string with each line indented by 4 spaces
         * (except the first line).
         */
        private static String toIndentedString(Object o) {
            if (o == null) {
                return "null";
            }
            return o.toString().replace("\n", "\n    ");
        }
    }
}