package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.entity.Product;

public interface Wrapper<T> {

    T retrieveInstitution();

    // Method used for additional checks
    Uni<Onboarding> customValidation(Product product);

    // Method used to check correspondence between registry and onboarding data
    Uni<Boolean> isValid();

    Onboarding getOnboarding();

    Wrapper setRegistryResource(T registryResource);
}

