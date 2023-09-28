package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class InvalidParam  {

    private String name;
    private String reason;

    /**
    * Invalid parameter name.
    * @return name
    **/
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Set name
     **/
    public void setName(String name) {
        this.name = name;
    }

    public InvalidParam name(String name) {
        this.name = name;
        return this;
    }

    /**
    * Invalid parameter reason.
    * @return reason
    **/
    @JsonProperty("reason")
    public String getReason() {
        return reason;
    }

    /**
     * Set reason
     **/
    public void setReason(String reason) {
        this.reason = reason;
    }

    public InvalidParam reason(String reason) {
        this.reason = reason;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class InvalidParam {\n");

        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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
    public static class InvalidParamQueryParam  {

        @jakarta.ws.rs.QueryParam("name")
        private String name;
        @jakarta.ws.rs.QueryParam("reason")
        private String reason;

        /**
        * Invalid parameter name.
        * @return name
        **/
        @JsonProperty("name")
        public String getName() {
            return name;
        }

        /**
         * Set name
         **/
        public void setName(String name) {
            this.name = name;
        }

        public InvalidParamQueryParam name(String name) {
            this.name = name;
            return this;
        }

        /**
        * Invalid parameter reason.
        * @return reason
        **/
        @JsonProperty("reason")
        public String getReason() {
            return reason;
        }

        /**
         * Set reason
         **/
        public void setReason(String reason) {
            this.reason = reason;
        }

        public InvalidParamQueryParam reason(String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class InvalidParamQueryParam {\n");

            sb.append("    name: ").append(toIndentedString(name)).append("\n");
            sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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