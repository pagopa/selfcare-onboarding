package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class BackOfficeConfigurationsResource  {

    private String identityTokenAudience;
    private String url;

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

    public BackOfficeConfigurationsResource identityTokenAudience(String identityTokenAudience) {
        this.identityTokenAudience = identityTokenAudience;
        return this;
    }

    /**
    * URL that redirects to the back-office section, where is possible to manage the product
    * @return url
    **/
    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    /**
     * Set url
     **/
    public void setUrl(String url) {
        this.url = url;
    }

    public BackOfficeConfigurationsResource url(String url) {
        this.url = url;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class BackOfficeConfigurationsResource {\n");

        sb.append("    identityTokenAudience: ").append(toIndentedString(identityTokenAudience)).append("\n");
        sb.append("    url: ").append(toIndentedString(url)).append("\n");
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
    public static class BackOfficeConfigurationsResourceQueryParam  {

        @jakarta.ws.rs.QueryParam("identityTokenAudience")
        private String identityTokenAudience;
        @jakarta.ws.rs.QueryParam("url")
        private String url;

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

        public BackOfficeConfigurationsResourceQueryParam identityTokenAudience(String identityTokenAudience) {
            this.identityTokenAudience = identityTokenAudience;
            return this;
        }

        /**
        * URL that redirects to the back-office section, where is possible to manage the product
        * @return url
        **/
        @JsonProperty("url")
        public String getUrl() {
            return url;
        }

        /**
         * Set url
         **/
        public void setUrl(String url) {
            this.url = url;
        }

        public BackOfficeConfigurationsResourceQueryParam url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class BackOfficeConfigurationsResourceQueryParam {\n");

            sb.append("    identityTokenAudience: ").append(toIndentedString(identityTokenAudience)).append("\n");
            sb.append("    url: ").append(toIndentedString(url)).append("\n");
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