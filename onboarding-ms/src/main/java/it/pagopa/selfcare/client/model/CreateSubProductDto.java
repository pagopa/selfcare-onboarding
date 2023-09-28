package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class CreateSubProductDto  {

    private String contractTemplatePath;
    private String contractTemplateVersion;
    private String id;
    private Map<String, ContractResource> institutionContractMappings;
    private String title;

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

    public CreateSubProductDto contractTemplatePath(String contractTemplatePath) {
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

    public CreateSubProductDto contractTemplateVersion(String contractTemplateVersion) {
        this.contractTemplateVersion = contractTemplateVersion;
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

    public CreateSubProductDto id(String id) {
        this.id = id;
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

    public CreateSubProductDto institutionContractMappings(Map<String, ContractResource> institutionContractMappings) {
        this.institutionContractMappings = institutionContractMappings;
        return this;
    }
    public CreateSubProductDto putInstitutionContractMappingsItem(String key, ContractResource institutionContractMappingsItem) {
        this.institutionContractMappings.put(key, institutionContractMappingsItem);
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

    public CreateSubProductDto title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CreateSubProductDto {\n");

        sb.append("    contractTemplatePath: ").append(toIndentedString(contractTemplatePath)).append("\n");
        sb.append("    contractTemplateVersion: ").append(toIndentedString(contractTemplateVersion)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    institutionContractMappings: ").append(toIndentedString(institutionContractMappings)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
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
    public static class CreateSubProductDtoQueryParam  {

        @jakarta.ws.rs.QueryParam("contractTemplatePath")
        private String contractTemplatePath;
        @jakarta.ws.rs.QueryParam("contractTemplateVersion")
        private String contractTemplateVersion;
        @jakarta.ws.rs.QueryParam("id")
        private String id;
        @jakarta.ws.rs.QueryParam("institutionContractMappings")
        private Map<String, ContractResource> institutionContractMappings = null;
        @jakarta.ws.rs.QueryParam("title")
        private String title;

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

        public CreateSubProductDtoQueryParam contractTemplatePath(String contractTemplatePath) {
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

        public CreateSubProductDtoQueryParam contractTemplateVersion(String contractTemplateVersion) {
            this.contractTemplateVersion = contractTemplateVersion;
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

        public CreateSubProductDtoQueryParam id(String id) {
            this.id = id;
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

        public CreateSubProductDtoQueryParam institutionContractMappings(Map<String, ContractResource> institutionContractMappings) {
            this.institutionContractMappings = institutionContractMappings;
            return this;
        }
        public CreateSubProductDtoQueryParam putInstitutionContractMappingsItem(String key, ContractResource institutionContractMappingsItem) {
            this.institutionContractMappings.put(key, institutionContractMappingsItem);
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

        public CreateSubProductDtoQueryParam title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class CreateSubProductDtoQueryParam {\n");

            sb.append("    contractTemplatePath: ").append(toIndentedString(contractTemplatePath)).append("\n");
            sb.append("    contractTemplateVersion: ").append(toIndentedString(contractTemplateVersion)).append("\n");
            sb.append("    id: ").append(toIndentedString(id)).append("\n");
            sb.append("    institutionContractMappings: ").append(toIndentedString(institutionContractMappings)).append("\n");
            sb.append("    title: ").append(toIndentedString(title)).append("\n");
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