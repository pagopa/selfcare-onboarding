package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.registry.client.ClientRegistryPDNDInfocamere;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.model.PDNDBusinessResource;

public class RegistryManagerPDNDInfocamere extends ClientRegistryPDNDInfocamere {

     /* if (InstitutionType.SCP == onboarding.getInstitution().getInstitutionType()
                || (InstitutionType.PRV == onboarding.getInstitution().getInstitutionType()
                        && !PROD_PAGOPA.getValue().equals(onboarding.getProductId()))) { */

    public RegistryManagerPDNDInfocamere(Onboarding onboarding, InfocamerePdndApi infocamerePdndApi) {
        super(onboarding, infocamerePdndApi);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return Uni.createFrom().item(onboarding);
    }

    @Override
    public Uni<Boolean> isValid() {
            if (!originPDNDInfocamere(onboarding, registryResource)) {
                return Uni.createFrom().failure(new InvalidRequestException("Field digitalAddress or description are not valid"));
            }
             return Uni.createFrom().item(true);
    }

    private boolean originPDNDInfocamere(Onboarding onboarding, PDNDBusinessResource pdndBusinessResource) {
        return onboarding.getInstitution().getDigitalAddress().equals(pdndBusinessResource.getDigitalAddress()) &&
                onboarding.getInstitution().getDescription().equals(pdndBusinessResource.getBusinessName());
    }

}