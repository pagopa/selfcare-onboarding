package it.pagopa.selfcare.onboarding.entity;


import io.quarkus.mongodb.panache.common.MongoEntity;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.LocalDateTime;
import java.util.List;


@MongoEntity(collection="onboardings")
public class Onboarding  {

    @BsonId
    private String id;
    private String productId;
    private List<String> testEnvProductIds;
    private WorkflowType workflowType;
    private Institution institution;
    private List<User> users;
    private List<AggregateInstitution> aggregates;
    private String pricingPlan;
    private Billing billing;
    private Boolean signContract;
    private LocalDateTime expiringDate;
    private OnboardingStatus status;
    private String userRequestUid;
    private String workflowInstanceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime deletedAt;
    private String reasonForReject;
    private Boolean isAggregator;
    private Aggregator aggregator;
    private String delegationId;
    private Boolean sendMailForImport;

    public Boolean getSendMailForImport() {
        return sendMailForImport;
    }

    public void setSendMailForImport(Boolean sendMailForImport) {
        this.sendMailForImport = sendMailForImport;
    }

    //This field is used in case of workflowType USER
    private String previousManagerId;

    public String getDelegationId() {
        return delegationId;
    }

    public void setDelegationId(String delegationId) {
        this.delegationId = delegationId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public LocalDateTime getExpiringDate() {
        return expiringDate;
    }

    public void setExpiringDate(LocalDateTime expiringDate) {
        this.expiringDate = expiringDate;
    }

    public OnboardingStatus getStatus() {
        return status;
    }

    public void setStatus(OnboardingStatus status) {
        this.status = status;
    }

    public String getUserRequestUid() {
        return userRequestUid;
    }

    public void setUserRequestUid(String userRequestUid) {
        this.userRequestUid = userRequestUid;
    }

    public WorkflowType getWorkflowType() {
        return workflowType;
    }

    public void setWorkflowType(WorkflowType workflowType) {
        this.workflowType = workflowType;
    }

    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    public String getReasonForReject(){ return  reasonForReject; }

    public void setReasonForReject(String reasonForReject) { this.reasonForReject = reasonForReject; }

    public List<String> getTestEnvProductIds() {
        return testEnvProductIds;
    }

    public void setTestEnvProductIds(List<String> testEnvproductIds) {
        this.testEnvProductIds = testEnvproductIds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public List<AggregateInstitution> getAggregates() { return aggregates; }

    public void setAggregates(List<AggregateInstitution> aggregates) { this.aggregates = aggregates; }

    public Boolean getIsAggregator() { return isAggregator; }

    public void setIsAggregator(Boolean isAggregator) { this.isAggregator = isAggregator; }

    public void setAggregator(Aggregator aggregator) {
        this.aggregator = aggregator;
    }

    public Aggregator getAggregator() {
        return aggregator;
    }

    public String getPreviousManagerId() {
        return previousManagerId;
    }

    public void setPreviousManagerId(String previousManagerId) {
        this.previousManagerId = previousManagerId;
    }

    @Override
    public String toString() {
        return "Onboarding{" +
                "id='" + id + '\'' +
                ", productId='" + productId + '\'' +
                ", testEnvProductIds=" + testEnvProductIds +
                ", workflowType=" + workflowType +
                ", institution=" + institution +
                ", users=" + users +
                ", pricingPlan='" + pricingPlan + '\'' +
                ", billing=" + billing +
                ", signContract=" + signContract +
                ", expiringDate=" + expiringDate +
                ", status=" + status +
                ", userRequestUid='" + userRequestUid + '\'' +
                ", workflowInstanceId='" + workflowInstanceId + '\'' +
                ", activatedAt=" + activatedAt +
                ", deletedAt=" + deletedAt +
                ", reasonForReject='" + reasonForReject + '\'' +
                ", aggregator=" + aggregator +
                ", aggregates=" + aggregates +
                ", isAggregator='" + isAggregator + '\'' +
                ", sendMailForImport='" + sendMailForImport + '\'' +
                '}';
    }
}
