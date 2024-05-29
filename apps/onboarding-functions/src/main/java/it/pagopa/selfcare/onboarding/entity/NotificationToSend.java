package it.pagopa.selfcare.onboarding.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationToSend {

    private String id;
    private String internalIstitutionID;
    private String product;
    private String state;
    private String filePath;
    private String fileName;
    private String contentType;
    private String onboardingTokenId;
    private String pricingPlan;
    //private InstitutionToNotify institution;
    private Billing billing;
    private OffsetDateTime createdAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime updatedAt;
    private QueueEvent notificationType;

    public String getInternalIstitutionID() {
        return internalIstitutionID;
    }

    public void setInternalIstitutionID(String internalIstitutionID) {
        this.internalIstitutionID = internalIstitutionID;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getOnboardingTokenId() {
        return onboardingTokenId;
    }

    public void setOnboardingTokenId(String onboardingTokenId) {
        this.onboardingTokenId = onboardingTokenId;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(OffsetDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public QueueEvent getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(QueueEvent notificationType) {
        this.notificationType = notificationType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "NotificationToSend{" +
                "id='" + id + '\'' +
                ", internalIstitutionID='" + internalIstitutionID + '\'' +
                ", product='" + product + '\'' +
                ", state='" + state + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileName='" + fileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", onboardingTokenId='" + onboardingTokenId + '\'' +
                ", pricingPlan='" + pricingPlan + '\'' +
                ", billing=" + billing +
                ", createdAt=" + createdAt +
                ", closedAt=" + closedAt +
                ", updatedAt=" + updatedAt +
                ", notificationType=" + notificationType +
                '}';
    }
}