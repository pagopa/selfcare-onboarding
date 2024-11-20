package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.registry.client.ClientRegistryADE;
import it.pagopa.selfcare.product.entity.Product;
import org.openapi.quarkus.party_registry_proxy_json.api.NationalRegistriesApi;
import org.openapi.quarkus.user_registry_json.api.UserApi;

public class RegistryManagerADE extends ClientRegistryADE {

  public RegistryManagerADE(Onboarding onboarding, NationalRegistriesApi nationalRegistriesApi, UserApi userApi) {
    super(onboarding, nationalRegistriesApi, userApi);
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
