package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.entity.Product;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;

public class RegistryManagerIPAAoo extends RegistryManagerIPAUo {

    public RegistryManagerIPAAoo(Onboarding onboarding, UoApi uoApi, AooApi aooApi) {
        super(onboarding, uoApi, aooApi);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return super.customValidation(product);
    }

    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }

}