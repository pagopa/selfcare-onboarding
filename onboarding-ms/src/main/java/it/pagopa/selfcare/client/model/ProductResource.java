package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ProductResource  {

    private Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations;
    private String contractTemplatePath;
    private OffsetDateTime contractTemplateUpdatedAt;
    private String contractTemplateVersion;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private Boolean delegable;
    private String depictImageUrl;
    private String description;
    private String id;
    private String identityTokenAudience;
    private String logo;
    private String logoBgColor;
    private OffsetDateTime modifiedAt;
    private UUID modifiedBy;
    private String parentId;
    private ProductOperations productOperations;
    private String roleManagementURL;
    private Map<String, ProductRoleInfoRes> roleMappings;

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
    * Environment-specific configurations for back-office redirection with Token Exchange
    * @return backOfficeEnvironmentConfigurations
    **/
    @JsonProperty("backOfficeEnvironmentConfigurations")
    public Map<String, BackOfficeConfigurationsResource> getBackOfficeEnvironmentConfigurations() {
        return backOfficeEnvironmentConfigurations;
    }

    /**
     * Set backOfficeEnvironmentConfigurations
     **/
    public void setBackOfficeEnvironmentConfigurations(Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations) {
        this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
    }

    public ProductResource backOfficeEnvironmentConfigurations(Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations) {
        this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
        return this;
    }
    public ProductResource putBackOfficeEnvironmentConfigurationsItem(String key, BackOfficeConfigurationsResource backOfficeEnvironmentConfigurationsItem) {
        this.backOfficeEnvironmentConfigurations.put(key, backOfficeEnvironmentConfigurationsItem);
        return this;
    }

    /**
    * The path of contract
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

    public ProductResource contractTemplatePath(String contractTemplatePath) {
        this.contractTemplatePath = contractTemplatePath;
        return this;
    }

    /**
    * Date the contract was postponed
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

    public ProductResource contractTemplateUpdatedAt(OffsetDateTime contractTemplateUpdatedAt) {
        this.contractTemplateUpdatedAt = contractTemplateUpdatedAt;
        return this;
    }

    /**
    * Version of the contract
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

    public ProductResource contractTemplateVersion(String contractTemplateVersion) {
        this.contractTemplateVersion = contractTemplateVersion;
        return this;
    }

    /**
    * Creation/activation date and time
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

    public ProductResource createdAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
    * User who created/activated the resource
    * @return createdBy
    **/
    @JsonProperty("createdBy")
    public UUID getCreatedBy() {
        return createdBy;
    }

    /**
     * Set createdBy
     **/
    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public ProductResource createdBy(UUID createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    /**
    * If a product is delegable to a technical partner 
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

    public ProductResource delegable(Boolean delegable) {
        this.delegable = delegable;
        return this;
    }

    /**
    * Product's depiction image url
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

    public ProductResource depictImageUrl(String depictImageUrl) {
        this.depictImageUrl = depictImageUrl;
        return this;
    }

    /**
    * Product's description
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

    public ProductResource description(String description) {
        this.description = description;
        return this;
    }

    /**
    * Product's unique identifier
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

    public ProductResource id(String id) {
        this.id = id;
        return this;
    }

    /**
    * Product's identity token audience
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

    public ProductResource identityTokenAudience(String identityTokenAudience) {
        this.identityTokenAudience = identityTokenAudience;
        return this;
    }

    /**
    * Product's logo url
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

    public ProductResource logo(String logo) {
        this.logo = logo;
        return this;
    }

    /**
    * Product logo's background color
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

    public ProductResource logoBgColor(String logoBgColor) {
        this.logoBgColor = logoBgColor;
        return this;
    }

    /**
    * Last modified date and time
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

    public ProductResource modifiedAt(OffsetDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
        return this;
    }

    /**
    * User who modified the resource
    * @return modifiedBy
    **/
    @JsonProperty("modifiedBy")
    public UUID getModifiedBy() {
        return modifiedBy;
    }

    /**
     * Set modifiedBy
     **/
    public void setModifiedBy(UUID modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public ProductResource modifiedBy(UUID modifiedBy) {
        this.modifiedBy = modifiedBy;
        return this;
    }

    /**
    * Root parent of the sub product
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

    public ProductResource parentId(String parentId) {
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

    public ProductResource productOperations(ProductOperations productOperations) {
        this.productOperations = productOperations;
        return this;
    }

    /**
    * Url of the utilities management
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

    public ProductResource roleManagementURL(String roleManagementURL) {
        this.roleManagementURL = roleManagementURL;
        return this;
    }

    /**
    * Mappings between Party's and Product's role
    * @return roleMappings
    **/
    @JsonProperty("roleMappings")
    public Map<String, ProductRoleInfoRes> getRoleMappings() {
        return roleMappings;
    }

    /**
     * Set roleMappings
     **/
    public void setRoleMappings(Map<String, ProductRoleInfoRes> roleMappings) {
        this.roleMappings = roleMappings;
    }

    public ProductResource roleMappings(Map<String, ProductRoleInfoRes> roleMappings) {
        this.roleMappings = roleMappings;
        return this;
    }
    public ProductResource putRoleMappingsItem(String key, ProductRoleInfoRes roleMappingsItem) {
        this.roleMappings.put(key, roleMappingsItem);
        return this;
    }

    /**
    * Product's status
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

    public ProductResource status(StatusEnum status) {
        this.status = status;
        return this;
    }

    /**
    * Product's title
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

    public ProductResource title(String title) {
        this.title = title;
        return this;
    }

    /**
    * URL that redirects to the back-office section, where is possible to manage the product
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

    public ProductResource urlBO(String urlBO) {
        this.urlBO = urlBO;
        return this;
    }

    /**
    * URL that redirects to the public information webpage of the product
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

    public ProductResource urlPublic(String urlPublic) {
        this.urlPublic = urlPublic;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ProductResource {\n");

        sb.append("    backOfficeEnvironmentConfigurations: ").append(toIndentedString(backOfficeEnvironmentConfigurations)).append("\n");
        sb.append("    contractTemplatePath: ").append(toIndentedString(contractTemplatePath)).append("\n");
        sb.append("    contractTemplateUpdatedAt: ").append(toIndentedString(contractTemplateUpdatedAt)).append("\n");
        sb.append("    contractTemplateVersion: ").append(toIndentedString(contractTemplateVersion)).append("\n");
        sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
        sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
        sb.append("    delegable: ").append(toIndentedString(delegable)).append("\n");
        sb.append("    depictImageUrl: ").append(toIndentedString(depictImageUrl)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    identityTokenAudience: ").append(toIndentedString(identityTokenAudience)).append("\n");
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
    public static class ProductResourceQueryParam  {

        @jakarta.ws.rs.QueryParam("backOfficeEnvironmentConfigurations")
        private Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations = null;
        @jakarta.ws.rs.QueryParam("contractTemplatePath")
        private String contractTemplatePath;
        @jakarta.ws.rs.QueryParam("contractTemplateUpdatedAt")
        private OffsetDateTime contractTemplateUpdatedAt;
        @jakarta.ws.rs.QueryParam("contractTemplateVersion")
        private String contractTemplateVersion;
        @jakarta.ws.rs.QueryParam("createdAt")
        private OffsetDateTime createdAt;
        @jakarta.ws.rs.QueryParam("createdBy")
        private UUID createdBy;
        @jakarta.ws.rs.QueryParam("delegable")
        private Boolean delegable;
        @jakarta.ws.rs.QueryParam("depictImageUrl")
        private String depictImageUrl;
        @jakarta.ws.rs.QueryParam("description")
        private String description;
        @jakarta.ws.rs.QueryParam("id")
        private String id;
        @jakarta.ws.rs.QueryParam("identityTokenAudience")
        private String identityTokenAudience;
        @jakarta.ws.rs.QueryParam("logo")
        private String logo;
        @jakarta.ws.rs.QueryParam("logoBgColor")
        private String logoBgColor;
        @jakarta.ws.rs.QueryParam("modifiedAt")
        private OffsetDateTime modifiedAt;
        @jakarta.ws.rs.QueryParam("modifiedBy")
        private UUID modifiedBy;
        @jakarta.ws.rs.QueryParam("parentId")
        private String parentId;
        @jakarta.ws.rs.QueryParam("productOperations")
        private ProductOperations productOperations;
        @jakarta.ws.rs.QueryParam("roleManagementURL")
        private String roleManagementURL;
        @jakarta.ws.rs.QueryParam("roleMappings")
        private Map<String, ProductRoleInfoRes> roleMappings = null;

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
        * Environment-specific configurations for back-office redirection with Token Exchange
        * @return backOfficeEnvironmentConfigurations
        **/
        @JsonProperty("backOfficeEnvironmentConfigurations")
        public Map<String, BackOfficeConfigurationsResource> getBackOfficeEnvironmentConfigurations() {
            return backOfficeEnvironmentConfigurations;
        }

        /**
         * Set backOfficeEnvironmentConfigurations
         **/
        public void setBackOfficeEnvironmentConfigurations(Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations) {
            this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
        }

        public ProductResourceQueryParam backOfficeEnvironmentConfigurations(Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations) {
            this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
            return this;
        }
        public ProductResourceQueryParam putBackOfficeEnvironmentConfigurationsItem(String key, BackOfficeConfigurationsResource backOfficeEnvironmentConfigurationsItem) {
            this.backOfficeEnvironmentConfigurations.put(key, backOfficeEnvironmentConfigurationsItem);
            return this;
        }

        /**
        * The path of contract
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

        public ProductResourceQueryParam contractTemplatePath(String contractTemplatePath) {
            this.contractTemplatePath = contractTemplatePath;
            return this;
        }

        /**
        * Date the contract was postponed
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

        public ProductResourceQueryParam contractTemplateUpdatedAt(OffsetDateTime contractTemplateUpdatedAt) {
            this.contractTemplateUpdatedAt = contractTemplateUpdatedAt;
            return this;
        }

        /**
        * Version of the contract
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

        public ProductResourceQueryParam contractTemplateVersion(String contractTemplateVersion) {
            this.contractTemplateVersion = contractTemplateVersion;
            return this;
        }

        /**
        * Creation/activation date and time
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

        public ProductResourceQueryParam createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
        * User who created/activated the resource
        * @return createdBy
        **/
        @JsonProperty("createdBy")
        public UUID getCreatedBy() {
            return createdBy;
        }

        /**
         * Set createdBy
         **/
        public void setCreatedBy(UUID createdBy) {
            this.createdBy = createdBy;
        }

        public ProductResourceQueryParam createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        /**
        * If a product is delegable to a technical partner 
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

        public ProductResourceQueryParam delegable(Boolean delegable) {
            this.delegable = delegable;
            return this;
        }

        /**
        * Product's depiction image url
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

        public ProductResourceQueryParam depictImageUrl(String depictImageUrl) {
            this.depictImageUrl = depictImageUrl;
            return this;
        }

        /**
        * Product's description
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

        public ProductResourceQueryParam description(String description) {
            this.description = description;
            return this;
        }

        /**
        * Product's unique identifier
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

        public ProductResourceQueryParam id(String id) {
            this.id = id;
            return this;
        }

        /**
        * Product's identity token audience
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

        public ProductResourceQueryParam identityTokenAudience(String identityTokenAudience) {
            this.identityTokenAudience = identityTokenAudience;
            return this;
        }

        /**
        * Product's logo url
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

        public ProductResourceQueryParam logo(String logo) {
            this.logo = logo;
            return this;
        }

        /**
        * Product logo's background color
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

        public ProductResourceQueryParam logoBgColor(String logoBgColor) {
            this.logoBgColor = logoBgColor;
            return this;
        }

        /**
        * Last modified date and time
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

        public ProductResourceQueryParam modifiedAt(OffsetDateTime modifiedAt) {
            this.modifiedAt = modifiedAt;
            return this;
        }

        /**
        * User who modified the resource
        * @return modifiedBy
        **/
        @JsonProperty("modifiedBy")
        public UUID getModifiedBy() {
            return modifiedBy;
        }

        /**
         * Set modifiedBy
         **/
        public void setModifiedBy(UUID modifiedBy) {
            this.modifiedBy = modifiedBy;
        }

        public ProductResourceQueryParam modifiedBy(UUID modifiedBy) {
            this.modifiedBy = modifiedBy;
            return this;
        }

        /**
        * Root parent of the sub product
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

        public ProductResourceQueryParam parentId(String parentId) {
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

        public ProductResourceQueryParam productOperations(ProductOperations productOperations) {
            this.productOperations = productOperations;
            return this;
        }

        /**
        * Url of the utilities management
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

        public ProductResourceQueryParam roleManagementURL(String roleManagementURL) {
            this.roleManagementURL = roleManagementURL;
            return this;
        }

        /**
        * Mappings between Party's and Product's role
        * @return roleMappings
        **/
        @JsonProperty("roleMappings")
        public Map<String, ProductRoleInfoRes> getRoleMappings() {
            return roleMappings;
        }

        /**
         * Set roleMappings
         **/
        public void setRoleMappings(Map<String, ProductRoleInfoRes> roleMappings) {
            this.roleMappings = roleMappings;
        }

        public ProductResourceQueryParam roleMappings(Map<String, ProductRoleInfoRes> roleMappings) {
            this.roleMappings = roleMappings;
            return this;
        }
        public ProductResourceQueryParam putRoleMappingsItem(String key, ProductRoleInfoRes roleMappingsItem) {
            this.roleMappings.put(key, roleMappingsItem);
            return this;
        }

        /**
        * Product's status
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

        public ProductResourceQueryParam status(StatusEnum status) {
            this.status = status;
            return this;
        }

        /**
        * Product's title
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

        public ProductResourceQueryParam title(String title) {
            this.title = title;
            return this;
        }

        /**
        * URL that redirects to the back-office section, where is possible to manage the product
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

        public ProductResourceQueryParam urlBO(String urlBO) {
            this.urlBO = urlBO;
            return this;
        }

        /**
        * URL that redirects to the public information webpage of the product
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

        public ProductResourceQueryParam urlPublic(String urlPublic) {
            this.urlPublic = urlPublic;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class ProductResourceQueryParam {\n");

            sb.append("    backOfficeEnvironmentConfigurations: ").append(toIndentedString(backOfficeEnvironmentConfigurations)).append("\n");
            sb.append("    contractTemplatePath: ").append(toIndentedString(contractTemplatePath)).append("\n");
            sb.append("    contractTemplateUpdatedAt: ").append(toIndentedString(contractTemplateUpdatedAt)).append("\n");
            sb.append("    contractTemplateVersion: ").append(toIndentedString(contractTemplateVersion)).append("\n");
            sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
            sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
            sb.append("    delegable: ").append(toIndentedString(delegable)).append("\n");
            sb.append("    depictImageUrl: ").append(toIndentedString(depictImageUrl)).append("\n");
            sb.append("    description: ").append(toIndentedString(description)).append("\n");
            sb.append("    id: ").append(toIndentedString(id)).append("\n");
            sb.append("    identityTokenAudience: ").append(toIndentedString(identityTokenAudience)).append("\n");
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