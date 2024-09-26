package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import static it.pagopa.selfcare.onboarding.constants.CustomError.UO_NOT_FOUND;

public class WrapperIPA extends BaseWrapper<Uni<InstitutionResource>> {

    protected UoApi uoClient;
    private  final org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi client;

    public WrapperIPA(Onboarding onboarding, InstitutionApi institutionApi, UoApi uoApi) {
        super(onboarding);
        registryResource = retrieveInstitution();
        client = institutionApi;
        uoClient = uoApi;
    }

    @Override
    public Uni<InstitutionResource> retrieveInstitution() {
        return client.findInstitutionUsingGET(onboarding.getInstitution().getId(), "", null);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return Uni.createFrom().item(onboarding);
    }


    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }

    public Uni<UOResource> getUoFromRecipientCode(String recipientCode) {
        return uoClient.findByUnicodeUsingGET1(recipientCode, null)
                .onFailure(WebApplicationException.class)
                .recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(
                        String.format(UO_NOT_FOUND.getMessage(),
                                recipientCode
                        )))
                        : Uni.createFrom().failure(ex));
    }
}
