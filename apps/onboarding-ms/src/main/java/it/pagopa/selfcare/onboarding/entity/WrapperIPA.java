package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;

public class WrapperIPA extends Wrapper<Uni<InstitutionResource>> {

    protected UoApi uoClient;
    private  org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi client;

    public WrapperIPA(Onboarding onboarding, InstitutionApi institutionApi, UoApi uoApi) {
        super(onboarding);
        registryResource = retrieveInstitution();
        client = institutionApi;
        uoClient = uoApi;
    }

    @Override
    public Uni<InstitutionResource> retrieveInstitution() {
        return client.findInstitutionUsingGET("", "", null);
    }

    @Override
    boolean customValidation() {
        return false;
    }

    @Override
    boolean isValid() {
        return true;
    }
}
