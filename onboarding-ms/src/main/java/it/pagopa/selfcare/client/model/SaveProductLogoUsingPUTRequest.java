package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;

@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class SaveProductLogoUsingPUTRequest  {

    private File logo;

    /**
    * Product's logo image
    * @return logo
    **/
    @JsonProperty("logo")
    public File getLogo() {
        return logo;
    }

    /**
     * Set logo
     **/
    public void setLogo(File logo) {
        this.logo = logo;
    }

    public SaveProductLogoUsingPUTRequest logo(File logo) {
        this.logo = logo;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SaveProductLogoUsingPUTRequest {\n");

        sb.append("    logo: ").append(toIndentedString(logo)).append("\n");
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
    public static class SaveProductLogoUsingPUTRequestQueryParam  {

        @jakarta.ws.rs.QueryParam("logo")
        private File logo;

        /**
        * Product's logo image
        * @return logo
        **/
        @JsonProperty("logo")
        public File getLogo() {
            return logo;
        }

        /**
         * Set logo
         **/
        public void setLogo(File logo) {
            this.logo = logo;
        }

        public SaveProductLogoUsingPUTRequestQueryParam logo(File logo) {
            this.logo = logo;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class SaveProductLogoUsingPUTRequestQueryParam {\n");

            sb.append("    logo: ").append(toIndentedString(logo)).append("\n");
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