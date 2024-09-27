package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InsuranceCompaniesApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InsuranceCompanyResource;

import static it.pagopa.selfcare.onboarding.constants.CustomError.AOO_NOT_FOUND;

public class WrapperIVASS extends BaseWrapper<Uni<InsuranceCompanyResource>> {

    private final InsuranceCompaniesApi client;

    public WrapperIVASS(Onboarding onboarding, InsuranceCompaniesApi insuranceApi) {
        super(onboarding);
        client = insuranceApi;
        registryResource = retrieveInstitution();
    }

    public Uni<InsuranceCompanyResource> retrieveInstitution() {
        return client.searchByTaxCodeUsingGET(onboarding.getInstitution().getTaxCode())
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format(AOO_NOT_FOUND.getMessage(), onboarding.getInstitution().getSubunitCode())))
                        : Uni.createFrom().failure(ex));
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