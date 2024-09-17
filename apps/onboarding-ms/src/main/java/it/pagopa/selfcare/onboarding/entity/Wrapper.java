package it.pagopa.selfcare.onboarding.entity;

public abstract class Wrapper<T> {

    protected Onboarding onboarding;
    protected Institution institution;
    protected T registryResource;

    public Wrapper(Onboarding onboarding) {
        this.onboarding = onboarding;
    }

    abstract T retrieveInstitution();

    abstract boolean customValidation();

    abstract boolean isValid();
}
