package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.registry.client.ClientRegistryInfocamere;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamereApi;
import org.openapi.quarkus.party_registry_proxy_json.model.BusinessesResource;

public class RegistryManagerInfocamere extends ClientRegistryInfocamere {

  public RegistryManagerInfocamere(Onboarding onboarding, InfocamereApi infocamereApi) {
    super(onboarding, infocamereApi);
  }

  @Override
  public Uni<Onboarding> customValidation(Product product) {
    return Uni.createFrom().item(onboarding);
  }

  @Override
  public Uni<Boolean> isValid() {
    if (!originInfocamere(onboarding, registryResource)) {
      return Uni.createFrom()
          .failure(new InvalidRequestException("Field description is not valid"));
    }
    return Uni.createFrom().item(true);
  }

  private boolean originInfocamere(Onboarding onboarding, BusinessesResource businessesResource) {
    return businessesResource.getBusinesses().stream()
        .anyMatch(
            businessResource ->
                businessResource
                    .getBusinessName()
                    .equals(onboarding.getInstitution().getDescription()));
  }
}
