package it.pagopa.selfcare.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ProductRoleInfoRes  {

    private Boolean multiroleAllowed;
    private List<ProductRole> roles = new ArrayList<>();

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

    public ProductRoleInfoRes multiroleAllowed(Boolean multiroleAllowed) {
        this.multiroleAllowed = multiroleAllowed;
        return this;
    }

    /**
    * Available product roles
    * @return roles
    **/
    @JsonProperty("roles")
    public List<ProductRole> getRoles() {
        return roles;
    }

    /**
     * Set roles
     **/
    public void setRoles(List<ProductRole> roles) {
        this.roles = roles;
    }

    public ProductRoleInfoRes roles(List<ProductRole> roles) {
        this.roles = roles;
        return this;
    }
    public ProductRoleInfoRes addRolesItem(ProductRole rolesItem) {
        this.roles.add(rolesItem);
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ProductRoleInfoRes {\n");

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
    public static class ProductRoleInfoResQueryParam  {

        @jakarta.ws.rs.QueryParam("multiroleAllowed")
        private Boolean multiroleAllowed;
        @jakarta.ws.rs.QueryParam("roles")
        private List<ProductRole> roles = null;

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

        public ProductRoleInfoResQueryParam multiroleAllowed(Boolean multiroleAllowed) {
            this.multiroleAllowed = multiroleAllowed;
            return this;
        }

        /**
        * Available product roles
        * @return roles
        **/
        @JsonProperty("roles")
        public List<ProductRole> getRoles() {
            return roles;
        }

        /**
         * Set roles
         **/
        public void setRoles(List<ProductRole> roles) {
            this.roles = roles;
        }

        public ProductRoleInfoResQueryParam roles(List<ProductRole> roles) {
            this.roles = roles;
            return this;
        }
        public ProductRoleInfoResQueryParam addRolesItem(ProductRole rolesItem) {
            this.roles.add(rolesItem);
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class ProductRoleInfoResQueryParam {\n");

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