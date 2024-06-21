package it.pagopa.selfcare.onboarding.dto;

public class OnboardingCountResult {

    private String prod;
    private long countCompleted;
    private long countDeleted;

    public OnboardingCountResult( String prod,long countCompleted, long countDeleted) {
        this.prod = prod;
        this.countCompleted = countCompleted;
        this.countDeleted = countDeleted;
    }

    public String getProd() {
        return prod;
    }

    public void setProd(String prod) {
        this.prod = prod;
    }


    public long getCountCompleted() {
        return countCompleted;
    }

    public void setCountCompleted(long count) {
        this.countCompleted = countCompleted;
    }

    public long getCountDeleted() {
        return countDeleted;
    }

    public void setCountDeleted(long count) {
        this.countDeleted = countDeleted;
    }
}
