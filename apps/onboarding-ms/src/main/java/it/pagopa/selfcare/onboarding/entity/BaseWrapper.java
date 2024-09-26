package it.pagopa.selfcare.onboarding.entity;


public abstract class BaseWrapper<T> implements Wrapper {

    protected Onboarding onboarding;
    protected T registryResource;

    public BaseWrapper(Onboarding onboarding) {
        this.onboarding = onboarding;
    }

    public void setOnboarding(Onboarding onboarding) {
        this.onboarding = onboarding;
    }

    public Onboarding getOnboarding() {
        return onboarding;
    }

}
