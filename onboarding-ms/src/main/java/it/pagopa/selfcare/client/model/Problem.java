package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
/**
  * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
 **/
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class Problem  {

    /**
      * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
     **/
    private String detail;
    /**
      * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
     **/
    private String instance;
    /**
      * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
     **/
    private List<InvalidParam> invalidParams;
    /**
      * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
     **/
    private Integer status;
    /**
      * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
     **/
    private String title;
    /**
      * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
     **/
    private String type;

    /**
    * Human-readable description of this specific problem.
    * @return detail
    **/
    @JsonProperty("detail")
    public String getDetail() {
        return detail;
    }

    /**
     * Set detail
     **/
    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Problem detail(String detail) {
        this.detail = detail;
        return this;
    }

    /**
    * A URI that describes where the problem occurred.
    * @return instance
    **/
    @JsonProperty("instance")
    public String getInstance() {
        return instance;
    }

    /**
     * Set instance
     **/
    public void setInstance(String instance) {
        this.instance = instance;
    }

    public Problem instance(String instance) {
        this.instance = instance;
        return this;
    }

    /**
    * A list of invalid parameters details.
    * @return invalidParams
    **/
    @JsonProperty("invalidParams")
    public List<InvalidParam> getInvalidParams() {
        return invalidParams;
    }

    /**
     * Set invalidParams
     **/
    public void setInvalidParams(List<InvalidParam> invalidParams) {
        this.invalidParams = invalidParams;
    }

    public Problem invalidParams(List<InvalidParam> invalidParams) {
        this.invalidParams = invalidParams;
        return this;
    }
    public Problem addInvalidParamsItem(InvalidParam invalidParamsItem) {
        this.invalidParams.add(invalidParamsItem);
        return this;
    }

    /**
    * The HTTP status code.
    * @return status
    **/
    @JsonProperty("status")
    public Integer getStatus() {
        return status;
    }

    /**
     * Set status
     **/
    public void setStatus(Integer status) {
        this.status = status;
    }

    public Problem status(Integer status) {
        this.status = status;
        return this;
    }

    /**
    * Short human-readable summary of the problem.
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

    public Problem title(String title) {
        this.title = title;
        return this;
    }

    /**
    * A URL to a page with more details regarding the problem.
    * @return type
    **/
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    /**
     * Set type
     **/
    public void setType(String type) {
        this.type = type;
    }

    public Problem type(String type) {
        this.type = type;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Problem {\n");

        sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
        sb.append("    instance: ").append(toIndentedString(instance)).append("\n");
        sb.append("    invalidParams: ").append(toIndentedString(invalidParams)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
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
    /**
      * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
     **/
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
    public static class ProblemQueryParam  {

        /**
          * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
         **/
        @jakarta.ws.rs.QueryParam("detail")
        private String detail;
        /**
          * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
         **/
        @jakarta.ws.rs.QueryParam("instance")
        private String instance;
        /**
          * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
         **/
        @jakarta.ws.rs.QueryParam("invalidParams")
        private List<InvalidParam> invalidParams = null;
        /**
          * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
         **/
        @jakarta.ws.rs.QueryParam("status")
        private Integer status;
        /**
          * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
         **/
        @jakarta.ws.rs.QueryParam("title")
        private String title;
        /**
          * A \"problem detail\" as a way to carry machine-readable details of errors (https://datatracker.ietf.org/doc/html/rfc7807)
         **/
        @jakarta.ws.rs.QueryParam("type")
        private String type;

        /**
        * Human-readable description of this specific problem.
        * @return detail
        **/
        @JsonProperty("detail")
        public String getDetail() {
            return detail;
        }

        /**
         * Set detail
         **/
        public void setDetail(String detail) {
            this.detail = detail;
        }

        public ProblemQueryParam detail(String detail) {
            this.detail = detail;
            return this;
        }

        /**
        * A URI that describes where the problem occurred.
        * @return instance
        **/
        @JsonProperty("instance")
        public String getInstance() {
            return instance;
        }

        /**
         * Set instance
         **/
        public void setInstance(String instance) {
            this.instance = instance;
        }

        public ProblemQueryParam instance(String instance) {
            this.instance = instance;
            return this;
        }

        /**
        * A list of invalid parameters details.
        * @return invalidParams
        **/
        @JsonProperty("invalidParams")
        public List<InvalidParam> getInvalidParams() {
            return invalidParams;
        }

        /**
         * Set invalidParams
         **/
        public void setInvalidParams(List<InvalidParam> invalidParams) {
            this.invalidParams = invalidParams;
        }

        public ProblemQueryParam invalidParams(List<InvalidParam> invalidParams) {
            this.invalidParams = invalidParams;
            return this;
        }
        public ProblemQueryParam addInvalidParamsItem(InvalidParam invalidParamsItem) {
            this.invalidParams.add(invalidParamsItem);
            return this;
        }

        /**
        * The HTTP status code.
        * @return status
        **/
        @JsonProperty("status")
        public Integer getStatus() {
            return status;
        }

        /**
         * Set status
         **/
        public void setStatus(Integer status) {
            this.status = status;
        }

        public ProblemQueryParam status(Integer status) {
            this.status = status;
            return this;
        }

        /**
        * Short human-readable summary of the problem.
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

        public ProblemQueryParam title(String title) {
            this.title = title;
            return this;
        }

        /**
        * A URL to a page with more details regarding the problem.
        * @return type
        **/
        @JsonProperty("type")
        public String getType() {
            return type;
        }

        /**
         * Set type
         **/
        public void setType(String type) {
            this.type = type;
        }

        public ProblemQueryParam type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class ProblemQueryParam {\n");

            sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
            sb.append("    instance: ").append(toIndentedString(instance)).append("\n");
            sb.append("    invalidParams: ").append(toIndentedString(invalidParams)).append("\n");
            sb.append("    status: ").append(toIndentedString(status)).append("\n");
            sb.append("    title: ").append(toIndentedString(title)).append("\n");
            sb.append("    type: ").append(toIndentedString(type)).append("\n");
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