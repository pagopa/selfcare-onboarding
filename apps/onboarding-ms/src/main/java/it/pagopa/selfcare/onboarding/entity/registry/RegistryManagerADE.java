package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.registry.client.ClientRegistryADE;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import org.openapi.quarkus.party_registry_proxy_json.api.NationalRegistriesApi;
import org.openapi.quarkus.user_registry_json.api.UserApi;

import java.util.Objects;

public class RegistryManagerADE extends ClientRegistryADE {

  public RegistryManagerADE(
      Onboarding onboarding, NationalRegistriesApi nationalRegistriesApi, UserApi userApi) {
    super(onboarding, nationalRegistriesApi, userApi);
  }

  @Override
  public Uni<Onboarding> customValidation(Product product) {
    return Uni.createFrom().item(onboarding);
  }

  @Override
  public Uni<Boolean> isValid() {
    Boolean result = super.registryResource;
    if (Objects.isNull(result) || !result) {
      return Uni.createFrom()
          .failure(
              new InvalidRequestException(
                  String.format(
                      PNPG_INSTITUTION_REGISTRY_NOT_FOUND,
                      onboarding.getInstitution().getTaxCode())));
    }
    return Uni.createFrom().item(true);
  }
}
