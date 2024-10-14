package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.entity.Product;
import org.openapi.quarkus.party_registry_proxy_json.api.InsuranceCompaniesApi;

public class RegistryManagerIVASS extends ClientRegistryIVASS {

    public RegistryManagerIVASS(Onboarding onboarding, InsuranceCompaniesApi insuranceApi) {
        super(onboarding, insuranceApi);
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