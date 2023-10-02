package it.pagopa.selfcare.service.strategy;

public interface OnboardingValidationStrategy {

    boolean validate(String productId, String institutionExternalId);

}
