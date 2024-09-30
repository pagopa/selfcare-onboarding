package it.pagopa.selfcare.product.entity;

public enum PHASE_ADDITION_ALLOWED {

    //Phase on onboarding process
    ONBOARDING("onboarding"),

    //Phase on dashboard "Aggiunta Utenti" with any constraints
    DASHBOARD("dashboard"),

    //Phase on dashboard "Aggiunta Utenti" when a sign contract is needed
    DASHBOARD_ASYNC("dashboard-async");

    public final String value;

    PHASE_ADDITION_ALLOWED(String value){
        this.value = value;
    }
}
