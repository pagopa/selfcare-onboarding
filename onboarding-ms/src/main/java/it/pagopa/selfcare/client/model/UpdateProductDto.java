package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class UpdateProductDto  {

    private Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations;
    private String contractTemplatePath;
    private String contractTemplateVersion;
    private String description;
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

    public UpdateProductDto backOfficeEnvironmentConfigurations(Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations) {
        this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
        return this;
    }
    public UpdateProductDto putBackOfficeEnvironmentConfigurationsItem(String key, BackOfficeConfigurationsResource backOfficeEnvironmentConfigurationsItem) {
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

    public UpdateProductDto contractTemplatePath(String contractTemplatePath) {
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

    public UpdateProductDto contractTemplateVersion(String contractTemplateVersion) {
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

    public UpdateProductDto description(String description) {
        this.description = description;
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

    public UpdateProductDto identityTokenAudience(String identityTokenAudience) {
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

    public UpdateProductDto institutionContractMappings(Map<String, ContractResource> institutionContractMappings) {
        this.institutionContractMappings = institutionContractMappings;
        return this;
    }
    public UpdateProductDto putInstitutionContractMappingsItem(String key, ContractResource institutionContractMappingsItem) {
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

    public UpdateProductDto logoBgColor(String logoBgColor) {
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

    public UpdateProductDto roleMappings(Map<String, ProductRoleInfoReq> roleMappings) {
        this.roleMappings = roleMappings;
        return this;
    }
    public UpdateProductDto putRoleMappingsItem(String key, ProductRoleInfoReq roleMappingsItem) {
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

    public UpdateProductDto title(String title) {
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

    public UpdateProductDto urlBO(String urlBO) {
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

    public UpdateProductDto urlPublic(String urlPublic) {
        this.urlPublic = urlPublic;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class UpdateProductDto {\n");

        sb.append("    backOfficeEnvironmentConfigurations: ").append(toIndentedString(backOfficeEnvironmentConfigurations)).append("\n");
        sb.append("    contractTemplatePath: ").append(toIndentedString(contractTemplatePath)).append("\n");
        sb.append("    contractTemplateVersion: ").append(toIndentedString(contractTemplateVersion)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
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
    public static class UpdateProductDtoQueryParam  {

        @jakarta.ws.rs.QueryParam("backOfficeEnvironmentConfigurations")
        private Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations = null;
        @jakarta.ws.rs.QueryParam("contractTemplatePath")
        private String contractTemplatePath;
        @jakarta.ws.rs.QueryParam("contractTemplateVersion")
        private String contractTemplateVersion;
        @jakarta.ws.rs.QueryParam("description")
        private String description;
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

        public UpdateProductDtoQueryParam backOfficeEnvironmentConfigurations(Map<String, BackOfficeConfigurationsResource> backOfficeEnvironmentConfigurations) {
            this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
            return this;
        }
        public UpdateProductDtoQueryParam putBackOfficeEnvironmentConfigurationsItem(String key, BackOfficeConfigurationsResource backOfficeEnvironmentConfigurationsItem) {
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

        public UpdateProductDtoQueryParam contractTemplatePath(String contractTemplatePath) {
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

        public UpdateProductDtoQueryParam contractTemplateVersion(String contractTemplateVersion) {
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

        public UpdateProductDtoQueryParam description(String description) {
            this.description = description;
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

        public UpdateProductDtoQueryParam identityTokenAudience(String identityTokenAudience) {
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

        public UpdateProductDtoQueryParam institutionContractMappings(Map<String, ContractResource> institutionContractMappings) {
            this.institutionContractMappings = institutionContractMappings;
            return this;
        }
        public UpdateProductDtoQueryParam putInstitutionContractMappingsItem(String key, ContractResource institutionContractMappingsItem) {
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

        public UpdateProductDtoQueryParam logoBgColor(String logoBgColor) {
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

        public UpdateProductDtoQueryParam roleMappings(Map<String, ProductRoleInfoReq> roleMappings) {
            this.roleMappings = roleMappings;
            return this;
        }
        public UpdateProductDtoQueryParam putRoleMappingsItem(String key, ProductRoleInfoReq roleMappingsItem) {
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

        public UpdateProductDtoQueryParam title(String title) {
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

        public UpdateProductDtoQueryParam urlBO(String urlBO) {
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

        public UpdateProductDtoQueryParam urlPublic(String urlPublic) {
            this.urlPublic = urlPublic;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class UpdateProductDtoQueryParam {\n");

            sb.append("    backOfficeEnvironmentConfigurations: ").append(toIndentedString(backOfficeEnvironmentConfigurations)).append("\n");
            sb.append("    contractTemplatePath: ").append(toIndentedString(contractTemplatePath)).append("\n");
            sb.append("    contractTemplateVersion: ").append(toIndentedString(contractTemplateVersion)).append("\n");
            sb.append("    description: ").append(toIndentedString(description)).append("\n");
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