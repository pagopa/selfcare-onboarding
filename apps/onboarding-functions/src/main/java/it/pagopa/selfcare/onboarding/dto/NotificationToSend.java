package it.pagopa.selfcare.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.onboarding.entity.Billing;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationToSend {

    private String id;
    private String internalIstitutionID;
    private String institutionId;
    private String product;
    private String state;
    private String filePath;
    private String fileName;
    private String contentType;
    private String onboardingTokenId;
    private String pricingPlan;
    private InstitutionToNotify institution;
    private Billing billing;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private OffsetDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private OffsetDateTime closedAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private OffsetDateTime updatedAt;
    private QueueEvent notificationType;
    private NotificationType type;

    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }

    public InstitutionToNotify getInstitution() {
        return institution;
    }

    public void setInstitution(InstitutionToNotify institution) {
        this.institution = institution;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

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
        return this.contentType;
    }

    public void setContentType(String contractSigned) {
        String contractFileName = Objects.isNull(contractSigned) ? "" : Paths.get(contractSigned).getFileName().toString();
        if (contractFileName.isEmpty()) {
            this.contentType = "";
        } else if (contractFileName.endsWith(".p7m")) {
            this.contentType = "application/pkcs7-mime";
        } else if (contractFileName.endsWith(".pdf")) {
            this.contentType = "application/pdf";
        } else {
            this.contentType = "application/octet-stream";
        }
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