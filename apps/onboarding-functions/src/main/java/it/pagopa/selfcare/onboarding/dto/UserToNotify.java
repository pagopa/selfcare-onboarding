package it.pagopa.selfcare.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.onboarding.common.PartyRole;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserToNotify {

    private String userId;
    private String role;
    private String productRole;
    private String relationshipStatus;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProductRole() {
        return productRole;
    }

    public void setProductRole(String productRole) {
        this.productRole = productRole;
    }

    public String getRelationshipStatus() {
        return relationshipStatus;
    }

    public void setRelationshipStatus(String relationshipStatus) {
        this.relationshipStatus = relationshipStatus;
    }
}
