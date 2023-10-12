package it.pagopa.selfcare.utils;



public enum PricingPlan {

    FA("FAST"),
    BASE("BASE"),
    PREMIUM("PREMIUM");

    private final String value;

    PricingPlan(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
