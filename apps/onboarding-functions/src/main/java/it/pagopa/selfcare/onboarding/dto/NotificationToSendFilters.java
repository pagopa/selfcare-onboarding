package it.pagopa.selfcare.onboarding.dto;

public class NotificationToSendFilters {
    private String productId;
    private String institutionId;
    private String onboardingId;
    private String taxCode;
    private String status;
    private String from;
    private String to;
    private Integer page;
    private Integer size;

    public NotificationToSendFilters() {
    }

    public NotificationToSendFilters(String productId, String institutionId, String onboardingId, String taxCode, String status, String from, String to, Integer page, Integer size) {
        this.productId = productId;
        this.institutionId = institutionId;
        this.onboardingId = onboardingId;
        this.taxCode = taxCode;
        this.status = status;
        this.from = from;
        this.to = to;
        this.page = page;
        this.size = size;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }

    public String getOnboardingId() {
        return onboardingId;
    }

    public void setOnboardingId(String onboardingId) {
        this.onboardingId = onboardingId;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
