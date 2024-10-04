package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.model.PDNDBusinessResource;

public class WrapperPDNDInfocamere extends BaseWrapper<Uni<PDNDBusinessResource>> {

     /* if (InstitutionType.SCP == onboarding.getInstitution().getInstitutionType()
                || (InstitutionType.PRV == onboarding.getInstitution().getInstitutionType()
                        && !PROD_PAGOPA.getValue().equals(onboarding.getProductId()))) { */

    private final InfocamerePdndApi client;

    public WrapperPDNDInfocamere(Onboarding onboarding, InfocamerePdndApi infocamerePdndApi) {
        super(onboarding);
        client = infocamerePdndApi;
        registryResource = retrieveInstitution();
    }

    public Uni<PDNDBusinessResource> retrieveInstitution() {
        return client.institutionPdndByTaxCodeUsingGET(onboarding.getInstitution().getTaxCode())
                .onFailure(WebApplicationException.class)
                .recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(
                        String.format("Institution %s not found in the registry",
                                onboarding.getInstitution().getTaxCode()
                        )))
                        : Uni.createFrom().failure(ex));
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return Uni.createFrom().item(onboarding);
    }

    @Override
    public Uni<Boolean> isValid() {
         return registryResource.onItem().transformToUni(pdndBusinessResource -> {
            if (!originPDNDInfocamere(onboarding, pdndBusinessResource)) {
                return Uni.createFrom().failure(new InvalidRequestException("Field digitalAddress or description are not valid"));
            }
             return Uni.createFrom().item(true);
        });
    }

    private boolean originPDNDInfocamere(Onboarding onboarding, PDNDBusinessResource pdndBusinessResource) {
        return onboarding.getInstitution().getDigitalAddress().equals(pdndBusinessResource.getDigitalAddress()) &&
                onboarding.getInstitution().getDescription().equals(pdndBusinessResource.getBusinessName());
    }

}