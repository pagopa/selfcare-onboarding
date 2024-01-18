package it.pagopa.selfcare.product.entity;

import java.util.List;

public class ProductRoleInfo {

    private boolean multiroleAllowed;
    private List<ProductRole> roles;

    public boolean isMultiroleAllowed() {
        return multiroleAllowed;
    }

    public void setMultiroleAllowed(boolean multiroleAllowed) {
        this.multiroleAllowed = multiroleAllowed;
    }

    public List<ProductRole> getRoles() {
        return roles;
    }

    public void setRoles(List<ProductRole> roles) {
        this.roles = roles;
    }
}
