package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.entity.Product;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;

public class WrapperIPA extends WrapperUO {

    public WrapperIPA(Onboarding onboarding, UoApi uoApi) {
        super(onboarding, uoApi);
    }

    @Override
    public IPAEntity retrieveInstitution() {
        return null;
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
