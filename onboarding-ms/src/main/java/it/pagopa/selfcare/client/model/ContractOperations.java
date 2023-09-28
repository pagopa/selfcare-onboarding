package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ContractOperations  {

    private String contractTemplatePath;
    private OffsetDateTime contractTemplateUpdatedAt;
    private String contractTemplateVersion;

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

    public ContractOperations contractTemplatePath(String contractTemplatePath) {
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

    public ContractOperations contractTemplateUpdatedAt(OffsetDateTime contractTemplateUpdatedAt) {
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

    public ContractOperations contractTemplateVersion(String contractTemplateVersion) {
        this.contractTemplateVersion = contractTemplateVersion;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ContractOperations {\n");

        sb.append("    contractTemplatePath: ").append(toIndentedString(contractTemplatePath)).append("\n");
        sb.append("    contractTemplateUpdatedAt: ").append(toIndentedString(contractTemplateUpdatedAt)).append("\n");
        sb.append("    contractTemplateVersion: ").append(toIndentedString(contractTemplateVersion)).append("\n");
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
    public static class ContractOperationsQueryParam  {

        @jakarta.ws.rs.QueryParam("contractTemplatePath")
        private String contractTemplatePath;
        @jakarta.ws.rs.QueryParam("contractTemplateUpdatedAt")
        private OffsetDateTime contractTemplateUpdatedAt;
        @jakarta.ws.rs.QueryParam("contractTemplateVersion")
        private String contractTemplateVersion;

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

        public ContractOperationsQueryParam contractTemplatePath(String contractTemplatePath) {
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

        public ContractOperationsQueryParam contractTemplateUpdatedAt(OffsetDateTime contractTemplateUpdatedAt) {
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

        public ContractOperationsQueryParam contractTemplateVersion(String contractTemplateVersion) {
            this.contractTemplateVersion = contractTemplateVersion;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class ContractOperationsQueryParam {\n");

            sb.append("    contractTemplatePath: ").append(toIndentedString(contractTemplatePath)).append("\n");
            sb.append("    contractTemplateUpdatedAt: ").append(toIndentedString(contractTemplateUpdatedAt)).append("\n");
            sb.append("    contractTemplateVersion: ").append(toIndentedString(contractTemplateVersion)).append("\n");
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