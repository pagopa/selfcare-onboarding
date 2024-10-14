package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.entity.Product;

public class RegistryManagerSELC extends BaseRegistryManager<Uni<Object>> {

    public RegistryManagerSELC(Onboarding onboarding) {
        super(onboarding);
    }

    public Uni<Object> retrieveInstitution() {
      return Uni.createFrom().item(new Object());
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return Uni.createFrom().item(onboarding);
    }

    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }

}