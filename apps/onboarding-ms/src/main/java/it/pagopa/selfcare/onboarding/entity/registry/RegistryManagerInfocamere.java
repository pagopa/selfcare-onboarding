package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.IPAEntity;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.registry.client.ClientRegistryInfocamere;
import it.pagopa.selfcare.product.entity.Product;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamereApi;
import org.openapi.quarkus.party_registry_proxy_json.model.BusinessesResource;

import java.util.ArrayList;
import java.util.Objects;

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
    return Uni.createFrom().item(true);
  }
  
  @Override
  public RegistryManager<BusinessesResource> setResource(BusinessesResource registryResource) {
    this.registryResource = registryResource;
    if (Objects.isNull(onboarding.getInstitution().getGeographicTaxonomies())) {
      onboarding.getInstitution().setGeographicTaxonomies(new ArrayList<>());
    }
    return this;
  }
}
