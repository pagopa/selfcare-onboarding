package it.pagopa.selfcare.product.entity;

import java.util.List;

public class ProductRoleInfo {

    private boolean multiroleAllowed;

    /**
     * List of phases where addition of the role is allowed
     */
    private List<String> phasesAdditionAllowed;
    private List<ProductRole> roles;

    public boolean isMultiroleAllowed() {
        return multiroleAllowed;
    }

    public void setMultiroleAllowed(boolean multiroleAllowed) {
        this.multiroleAllowed = multiroleAllowed;
    }

    public List<String> getPhasesAdditionAllowed() {
        return phasesAdditionAllowed;
    }

    public void setPhasesAdditionAllowed(List<String> phasesAdditionAllowed) {
        this.phasesAdditionAllowed = phasesAdditionAllowed;
    }

    public List<ProductRole> getRoles() {
        return roles;
    }

    public void setRoles(List<ProductRole> roles) {
        this.roles = roles;
    }
}
