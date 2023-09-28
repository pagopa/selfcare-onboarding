package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ProductTreeResource  {

    private List<ProductResource> children;
    private ProductResource node;

    /**
    * Get children
    * @return children
    **/
    @JsonProperty("children")
    public List<ProductResource> getChildren() {
        return children;
    }

    /**
     * Set children
     **/
    public void setChildren(List<ProductResource> children) {
        this.children = children;
    }

    public ProductTreeResource children(List<ProductResource> children) {
        this.children = children;
        return this;
    }
    public ProductTreeResource addChildrenItem(ProductResource childrenItem) {
        this.children.add(childrenItem);
        return this;
    }

    /**
    * Get node
    * @return node
    **/
    @JsonProperty("node")
    public ProductResource getNode() {
        return node;
    }

    /**
     * Set node
     **/
    public void setNode(ProductResource node) {
        this.node = node;
    }

    public ProductTreeResource node(ProductResource node) {
        this.node = node;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ProductTreeResource {\n");

        sb.append("    children: ").append(toIndentedString(children)).append("\n");
        sb.append("    node: ").append(toIndentedString(node)).append("\n");
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
    public static class ProductTreeResourceQueryParam  {

        @jakarta.ws.rs.QueryParam("children")
        private List<ProductResource> children = null;
        @jakarta.ws.rs.QueryParam("node")
        private ProductResource node;

        /**
        * Get children
        * @return children
        **/
        @JsonProperty("children")
        public List<ProductResource> getChildren() {
            return children;
        }

        /**
         * Set children
         **/
        public void setChildren(List<ProductResource> children) {
            this.children = children;
        }

        public ProductTreeResourceQueryParam children(List<ProductResource> children) {
            this.children = children;
            return this;
        }
        public ProductTreeResourceQueryParam addChildrenItem(ProductResource childrenItem) {
            this.children.add(childrenItem);
            return this;
        }

        /**
        * Get node
        * @return node
        **/
        @JsonProperty("node")
        public ProductResource getNode() {
            return node;
        }

        /**
         * Set node
         **/
        public void setNode(ProductResource node) {
            this.node = node;
        }

        public ProductTreeResourceQueryParam node(ProductResource node) {
            this.node = node;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class ProductTreeResourceQueryParam {\n");

            sb.append("    children: ").append(toIndentedString(children)).append("\n");
            sb.append("    node: ").append(toIndentedString(node)).append("\n");
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