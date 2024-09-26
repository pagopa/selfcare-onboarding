package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;

import static it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType.EC;

@ApplicationScoped
public class RegistryResourceProvider {

    @RestClient
    @Inject
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionRegistryProxyApi;

    @RestClient
    @Inject
    AooApi aooApi;

    @RestClient
    @Inject
    UoApi uoApi;

    public Uni<Wrapper<?>> getResource(Onboarding onboarding) {
        return switch ((onboarding.getInstitution().getSubunitType() != null) ? onboarding.getInstitution().getSubunitType() : EC) {
            case AOO -> Uni.createFrom().item(new WrapperAOO(onboarding, aooApi));
            case UO -> Uni.createFrom().item(new WrapperUO(onboarding, uoApi));
            default -> Uni.createFrom().item(getResourceFromIPA(onboarding));
        };
    }

    private WrapperIPA getResourceFromIPA(Onboarding onboarding) {
        return switch (onboarding.getInstitution().getInstitutionType()) {
            case GSP -> new WrapperGPS(onboarding, institutionRegistryProxyApi, uoApi);
            case PT -> new WrapperPT(onboarding, institutionRegistryProxyApi, uoApi);
            default -> new WrapperPA(onboarding, institutionRegistryProxyApi, uoApi);
        };
    }
}
