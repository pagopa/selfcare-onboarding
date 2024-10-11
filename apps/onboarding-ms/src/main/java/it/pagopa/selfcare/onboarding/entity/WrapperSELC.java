package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.entity.Product;

public class WrapperSELC extends BaseWrapper<Uni<Object>> {

    private final Object client;

    public WrapperSELC(Onboarding onboarding, Object client) {
        super(onboarding);
        this.client = client;
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