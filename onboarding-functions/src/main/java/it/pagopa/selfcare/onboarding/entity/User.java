package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.commons.base.security.PartyRole;


public class User {

    private String id;
    private PartyRole role;
    private String ProductRole;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PartyRole getRole() {
        return role;
    }

    public void setRole(PartyRole role) {
        this.role = role;
    }

    public String getProductRole() {
        return ProductRole;
    }

    public void setProductRole(String productRole) {
        ProductRole = productRole;
    }
}
