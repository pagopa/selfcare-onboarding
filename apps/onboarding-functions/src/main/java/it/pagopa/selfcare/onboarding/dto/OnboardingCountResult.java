package it.pagopa.selfcare.onboarding.dto;

public class OnboardingCountResult {

    private String productId;
    private long countCompleted;
    private long countDeleted;

    public OnboardingCountResult(String productId, long countCompleted, long countDeleted) {
        this.productId = productId;
        this.countCompleted = countCompleted;
        this.countDeleted = countDeleted;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }


    public long getCountCompleted() {
        return countCompleted;
    }

    public void setCountCompleted(long countCompleted) {
        this.countCompleted = countCompleted;
    }

    public long getCountDeleted() {
        return countDeleted;
    }

    public void setCountDeleted(long countDeleted) {
        this.countDeleted = countDeleted;
    }
}
