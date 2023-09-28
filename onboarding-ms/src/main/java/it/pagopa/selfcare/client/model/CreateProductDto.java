package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class CreateProductDto  {

    private Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations;
    private String contractTemplatePath;
    private String contractTemplateVersion;
    private String description;
    private String id;
    private String identityTokenAudience;
    private Map<String, ContractResource> institutionContractMappings;
    private String logoBgColor;
    private Map<String, ProductRoleInfoReq> roleMappings = new HashMap<>();
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

    public CreateProductDto backOfficeEnvironmentConfigurations(Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations) {
        this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
        return this;
    }
    public CreateProductDto putBackOfficeEnvironmentConfigurationsItem(String key, BackOfficeConfigurationsResource backOfficeEnvironmentConfigurationsItem) {
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

    public CreateProductDto contractTemplatePath(String contractTemplatePath) {
        this.contractTemplatePath = contractTemplatePath;
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

    public CreateProductDto contractTemplateVersion(String contractTemplateVersion) {
        this.contractTemplateVersion = contractTemplateVersion;
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

    public CreateProductDto description(String description) {
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

    public CreateProductDto id(String id) {
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

    public CreateProductDto identityTokenAudience(String identityTokenAudience) {
        this.identityTokenAudience = identityTokenAudience;
        return this;
    }

    /**
    * Product contract based on institutionType
    * @return institutionContractMappings
    **/
    @JsonProperty("institutionContractMappings")
    public Map<String, ContractResource> getInstitutionContractMappings() {
        return institutionContractMappings;
    }

    /**
     * Set institutionContractMappings
     **/
    public void setInstitutionContractMappings(Map<String, ContractResource> institutionContractMappings) {
        this.institutionContractMappings = institutionContractMappings;
    }

    public CreateProductDto institutionContractMappings(Map<String, ContractResource> institutionContractMappings) {
        this.institutionContractMappings = institutionContractMappings;
        return this;
    }
    public CreateProductDto putInstitutionContractMappingsItem(String key, ContractResource institutionContractMappingsItem) {
        this.institutionContractMappings.put(key, institutionContractMappingsItem);
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

    public CreateProductDto logoBgColor(String logoBgColor) {
        this.logoBgColor = logoBgColor;
        return this;
    }

    /**
    * Mappings between Party's and Product's role
    * @return roleMappings
    **/
    @JsonProperty("roleMappings")
    public Map<String, ProductRoleInfoReq> getRoleMappings() {
        return roleMappings;
    }

    /**
     * Set roleMappings
     **/
    public void setRoleMappings(Map<String, ProductRoleInfoReq> roleMappings) {
        this.roleMappings = roleMappings;
    }

    public CreateProductDto roleMappings(Map<String, ProductRoleInfoReq> roleMappings) {
        this.roleMappings = roleMappings;
        return this;
    }
    public CreateProductDto putRoleMappingsItem(String key, ProductRoleInfoReq roleMappingsItem) {
        this.roleMappings.put(key, roleMappingsItem);
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

    public CreateProductDto title(String title) {
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

    public CreateProductDto urlBO(String urlBO) {
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

    public CreateProductDto urlPublic(String urlPublic) {
        this.urlPublic = urlPublic;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CreateProductDto {\n");

        sb.append("    backOfficeEnvironmentConfigurations: ").append(toIndentedString(backOfficeEnvironmentConfigurations)).append("\n");
        sb.append("    contractTemplatePath: ").append(toIndentedString(contractTemplatePath)).append("\n");
        sb.append("    contractTemplateVersion: ").append(toIndentedString(contractTemplateVersion)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    identityTokenAudience: ").append(toIndentedString(identityTokenAudience)).append("\n");
        sb.append("    institutionContractMappings: ").append(toIndentedString(institutionContractMappings)).append("\n");
        sb.append("    logoBgColor: ").append(toIndentedString(logoBgColor)).append("\n");
        sb.append("    roleMappings: ").append(toIndentedString(roleMappings)).append("\n");
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
    public static class CreateProductDtoQueryParam  {

        @jakarta.ws.rs.QueryParam("backOfficeEnvironmentConfigurations")
        private Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations = null;
        @jakarta.ws.rs.QueryParam("contractTemplatePath")
        private String contractTemplatePath;
        @jakarta.ws.rs.QueryParam("contractTemplateVersion")
        private String contractTemplateVersion;
        @jakarta.ws.rs.QueryParam("description")
        private String description;
        @jakarta.ws.rs.QueryParam("id")
        private String id;
        @jakarta.ws.rs.QueryParam("identityTokenAudience")
        private String identityTokenAudience;
        @jakarta.ws.rs.QueryParam("institutionContractMappings")
        private Map<String, ContractResource> institutionContractMappings = null;
        @jakarta.ws.rs.QueryParam("logoBgColor")
        private String logoBgColor;
        @jakarta.ws.rs.QueryParam("roleMappings")
        private Map<String, ProductRoleInfoReq> roleMappings = null;
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

        public CreateProductDtoQueryParam backOfficeEnvironmentConfigurations(Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations) {
            this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
            return this;
        }
        public CreateProductDtoQueryParam putBackOfficeEnvironmentConfigurationsItem(String key, BackOfficeConfigurationsResource backOfficeEnvironmentConfigurationsItem) {
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

        public CreateProductDtoQueryParam contractTemplatePath(String contractTemplatePath) {
            this.contractTemplatePath = contractTemplatePath;
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

        public CreateProductDtoQueryParam contractTemplateVersion(String contractTemplateVersion) {
            this.contractTemplateVersion = contractTemplateVersion;
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

        public CreateProductDtoQueryParam description(String description) {
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

        public CreateProductDtoQueryParam id(String id) {
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

        public CreateProductDtoQueryParam identityTokenAudience(String identityTokenAudience) {
            this.identityTokenAudience = identityTokenAudience;
            return this;
        }

        /**
        * Product contract based on institutionType
        * @return institutionContractMappings
        **/
        @JsonProperty("institutionContractMappings")
        public Map<String, ContractResource> getInstitutionContractMappings() {
            return institutionContractMappings;
        }

        /**
         * Set institutionContractMappings
         **/
        public void setInstitutionContractMappings(Map<String, ContractResource> institutionContractMappings) {
            this.institutionContractMappings = institutionContractMappings;
        }

        public CreateProductDtoQueryParam institutionContractMappings(Map<String, ContractResource> institutionContractMappings) {
            this.institutionContractMappings = institutionContractMappings;
            return this;
        }
        public CreateProductDtoQueryParam putInstitutionContractMappingsItem(String key, ContractResource institutionContractMappingsItem) {
            this.institutionContractMappings.put(key, institutionContractMappingsItem);
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

        public CreateProductDtoQueryParam logoBgColor(String logoBgColor) {
            this.logoBgColor = logoBgColor;
            return this;
        }

        /**
        * Mappings between Party's and Product's role
        * @return roleMappings
        **/
        @JsonProperty("roleMappings")
        public Map<String, ProductRoleInfoReq> getRoleMappings() {
            return roleMappings;
        }

        /**
         * Set roleMappings
         **/
        public void setRoleMappings(Map<String, ProductRoleInfoReq> roleMappings) {
            this.roleMappings = roleMappings;
        }

        public CreateProductDtoQueryParam roleMappings(Map<String, ProductRoleInfoReq> roleMappings) {
            this.roleMappings = roleMappings;
            return this;
        }
        public CreateProductDtoQueryParam putRoleMappingsItem(String key, ProductRoleInfoReq roleMappingsItem) {
            this.roleMappings.put(key, roleMappingsItem);
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

        public CreateProductDtoQueryParam title(String title) {
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

        public CreateProductDtoQueryParam urlBO(String urlBO) {
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

        public CreateProductDtoQueryParam urlPublic(String urlPublic) {
            this.urlPublic = urlPublic;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class CreateProductDtoQueryParam {\n");

            sb.append("    backOfficeEnvironmentConfigurations: ").append(toIndentedString(backOfficeEnvironmentConfigurations)).append("\n");
            sb.append("    contractTemplatePath: ").append(toIndentedString(contractTemplatePath)).append("\n");
            sb.append("    contractTemplateVersion: ").append(toIndentedString(contractTemplateVersion)).append("\n");
            sb.append("    description: ").append(toIndentedString(description)).append("\n");
            sb.append("    id: ").append(toIndentedString(id)).append("\n");
            sb.append("    identityTokenAudience: ").append(toIndentedString(identityTokenAudience)).append("\n");
            sb.append("    institutionContractMappings: ").append(toIndentedString(institutionContractMappings)).append("\n");
            sb.append("    logoBgColor: ").append(toIndentedString(logoBgColor)).append("\n");
            sb.append("    roleMappings: ").append(toIndentedString(roleMappings)).append("\n");
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