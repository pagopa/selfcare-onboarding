package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.openapi.quarkus.party_registry_proxy_json.api.*;

import static it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType.EC;

@ApplicationScoped
public class RegistryResourceProvider {

    private static final Logger LOG = Logger.getLogger(RegistryResourceProvider.class);

    @RestClient
    @Inject
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionRegistryProxyApi;

    @RestClient
    @Inject
    AooApi aooApi;

    @RestClient
    @Inject
    UoApi uoApi;

    @RestClient
    @Inject
    InfocamerePdndApi infocamerePdndApi;

    @RestClient
    @Inject
    InsuranceCompaniesApi insuranceCompaniesApi;

    @RestClient
    @Inject
    StationsApi stationsApi;

    public Uni<Wrapper<?>> getResource(Onboarding onboarding) {
        return switch (onboarding.getInstitution().getOrigin()) {
            case IPA -> getResourceFromIPA(onboarding);
            case PDND_INFOCAMERE -> Uni.createFrom().item(new WrapperInfocamere(onboarding, infocamerePdndApi));
            case ANAC -> Uni.createFrom().item(new WrapperANAC(onboarding, stationsApi));
            case IVASS -> Uni.createFrom().item(new WrapperIVASS(onboarding, insuranceCompaniesApi));
            default -> Uni.createFrom().nullItem();
        };
    }

    private Uni<Wrapper<?>> getResourceFromIPA(Onboarding onboarding) {
        return switch ((onboarding.getInstitution().getSubunitType() != null) ? onboarding.getInstitution().getSubunitType() : EC) {
            case AOO -> Uni.createFrom().item(new WrapperAOO(onboarding, aooApi));
            case UO -> Uni.createFrom().item(new WrapperUO(onboarding, uoApi));
            default -> Uni.createFrom().item(getResourceFromIPA(onboarding));
        };
    }
}
