package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;

@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class SaveProductDepictImageUsingPUTRequest  {

    private File depictImage;

    /**
    * Product's depict image
    * @return depictImage
    **/
    @JsonProperty("depictImage")
    public File getDepictImage() {
        return depictImage;
    }

    /**
     * Set depictImage
     **/
    public void setDepictImage(File depictImage) {
        this.depictImage = depictImage;
    }

    public SaveProductDepictImageUsingPUTRequest depictImage(File depictImage) {
        this.depictImage = depictImage;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SaveProductDepictImageUsingPUTRequest {\n");

        sb.append("    depictImage: ").append(toIndentedString(depictImage)).append("\n");
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
    public static class SaveProductDepictImageUsingPUTRequestQueryParam  {

        @jakarta.ws.rs.QueryParam("depictImage")
        private File depictImage;

        /**
        * Product's depict image
        * @return depictImage
        **/
        @JsonProperty("depictImage")
        public File getDepictImage() {
            return depictImage;
        }

        /**
         * Set depictImage
         **/
        public void setDepictImage(File depictImage) {
            this.depictImage = depictImage;
        }

        public SaveProductDepictImageUsingPUTRequestQueryParam depictImage(File depictImage) {
            this.depictImage = depictImage;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class SaveProductDepictImageUsingPUTRequestQueryParam {\n");

            sb.append("    depictImage: ").append(toIndentedString(depictImage)).append("\n");
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