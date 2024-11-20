package it.pagopa.selfcare.onboarding.entity.registry;


import it.pagopa.selfcare.onboarding.entity.Onboarding;

public abstract class BaseRegistryManager<T> implements RegistryManager<T> {

    protected static final String ADDITIONAL_INFORMATION_REQUIRED = "Additional Information is required when institutionType is GSP and productId is pagopa";
    protected static final String OTHER_NOTE_REQUIRED = "Other Note is required when other boolean are false";
    protected static final String BILLING_OR_RECIPIENT_CODE_REQUIRED = "Billing and/or recipient code are required";
    protected static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE = "Institution with external id '%s' is not allowed to onboard '%s' product because it is not delegable";
    protected static final String PARENT_TAX_CODE_IS_INVALID = "The tax code of the parent entity of the request does not match the tax code of the parent entity retrieved by IPA";
    protected static final String TAX_CODE_INVOICING_IS_INVALID = "The tax code invoicing of the request does not match any tax code of institutions' hierarchy";
    protected static final int DURATION_TIMEOUT = 5;
    protected static final int MAX_NUMBER_ATTEMPTS = 2;

    protected Onboarding onboarding;
    protected T registryResource;

    protected BaseRegistryManager(Onboarding onboarding) {
        this.onboarding = onboarding;
    }

    public void setOnboarding(Onboarding onboarding) {
        this.onboarding = onboarding;
    }

    public Onboarding getOnboarding() {
        return onboarding;
    }

    public RegistryManager<T> setResource(T registryResource) {
        this.registryResource = registryResource;
        return this;
    }
}
