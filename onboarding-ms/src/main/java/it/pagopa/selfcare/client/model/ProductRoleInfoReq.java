package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ProductRoleInfoReq  {

    private Boolean multiroleAllowed;
    private List<ProductRoleOperations> roles = new ArrayList<>();

    /**
    * Flag indicating if a User can have more than one product role
    * @return multiroleAllowed
    **/
    @JsonProperty("multiroleAllowed")
    public Boolean getMultiroleAllowed() {
        return multiroleAllowed;
    }

    /**
     * Set multiroleAllowed
     **/
    public void setMultiroleAllowed(Boolean multiroleAllowed) {
        this.multiroleAllowed = multiroleAllowed;
    }

    public ProductRoleInfoReq multiroleAllowed(Boolean multiroleAllowed) {
        this.multiroleAllowed = multiroleAllowed;
        return this;
    }

    /**
    * Available product roles
    * @return roles
    **/
    @JsonProperty("roles")
    public List<ProductRoleOperations> getRoles() {
        return roles;
    }

    /**
     * Set roles
     **/
    public void setRoles(List<ProductRoleOperations> roles) {
        this.roles = roles;
    }

    public ProductRoleInfoReq roles(List<ProductRoleOperations> roles) {
        this.roles = roles;
        return this;
    }
    public ProductRoleInfoReq addRolesItem(ProductRoleOperations rolesItem) {
        this.roles.add(rolesItem);
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ProductRoleInfoReq {\n");

        sb.append("    multiroleAllowed: ").append(toIndentedString(multiroleAllowed)).append("\n");
        sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
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
    public static class ProductRoleInfoReqQueryParam  {

        @jakarta.ws.rs.QueryParam("multiroleAllowed")
        private Boolean multiroleAllowed;
        @jakarta.ws.rs.QueryParam("roles")
        private List<ProductRoleOperations> roles = null;

        /**
        * Flag indicating if a User can have more than one product role
        * @return multiroleAllowed
        **/
        @JsonProperty("multiroleAllowed")
        public Boolean getMultiroleAllowed() {
            return multiroleAllowed;
        }

        /**
         * Set multiroleAllowed
         **/
        public void setMultiroleAllowed(Boolean multiroleAllowed) {
            this.multiroleAllowed = multiroleAllowed;
        }

        public ProductRoleInfoReqQueryParam multiroleAllowed(Boolean multiroleAllowed) {
            this.multiroleAllowed = multiroleAllowed;
            return this;
        }

        /**
        * Available product roles
        * @return roles
        **/
        @JsonProperty("roles")
        public List<ProductRoleOperations> getRoles() {
            return roles;
        }

        /**
         * Set roles
         **/
        public void setRoles(List<ProductRoleOperations> roles) {
            this.roles = roles;
        }

        public ProductRoleInfoReqQueryParam roles(List<ProductRoleOperations> roles) {
            this.roles = roles;
            return this;
        }
        public ProductRoleInfoReqQueryParam addRolesItem(ProductRoleOperations rolesItem) {
            this.roles.add(rolesItem);
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class ProductRoleInfoReqQueryParam {\n");

            sb.append("    multiroleAllowed: ").append(toIndentedString(multiroleAllowed)).append("\n");
            sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
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