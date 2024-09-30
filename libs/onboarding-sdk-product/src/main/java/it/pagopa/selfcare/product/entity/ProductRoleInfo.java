package it.pagopa.selfcare.product.entity;

import java.util.List;

public class ProductRoleInfo {

    private boolean multiroleAllowed;

    /**
     * List of phases where addition of the role is allowed
     */
    private List<PHASE_ADDITION_ALLOWED> phasesAdditionAllowed;
    private List<ProductRole> roles;

    public boolean isMultiroleAllowed() {
        return multiroleAllowed;
    }

    public void setMultiroleAllowed(boolean multiroleAllowed) {
        this.multiroleAllowed = multiroleAllowed;
    }

    public List<PHASE_ADDITION_ALLOWED> getPhasesAdditionAllowed() {
        return phasesAdditionAllowed;
    }

    public void setPhasesAdditionAllowed(List<PHASE_ADDITION_ALLOWED> phasesAdditionAllowed) {
        this.phasesAdditionAllowed = phasesAdditionAllowed;
    }

    public List<ProductRole> getRoles() {
        return roles;
    }

    public void setRoles(List<ProductRole> roles) {
        this.roles = roles;
    }
}
