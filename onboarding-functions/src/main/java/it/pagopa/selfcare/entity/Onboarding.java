package it.pagopa.selfcare.entity;


import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;


@MongoEntity(collection="onboardings")
public class Onboarding {

    public ObjectId id;

    private String productId;
    private Institution institution;
    private List<User> users;
    private String pricingPlan;
    private Billing billing;
    private Boolean signContract;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public String getPricingPlan() {
        return pricingPlan;
    }

    public void setPricingPlan(String pricingPlan) {
        this.pricingPlan = pricingPlan;
    }

    public Billing getBilling() {
        return billing;
    }

    public void setBilling(Billing billing) {
        this.billing = billing;
    }

    public Boolean getSignContract() {
        return signContract;
    }

    public void setSignContract(Boolean signContract) {
        this.signContract = signContract;
    }
}
