package it.pagopa.selfcare.onboarding.entity;


import it.pagopa.selfcare.onboarding.common.PartyRole;

public class User {

    private String id;
    private PartyRole role;
    private String productRole;

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
        return productRole;
    }

    public void setProductRole(String productRole) {
        this.productRole = productRole;
    }
}
