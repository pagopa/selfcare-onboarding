package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.party_registry_proxy_json.api.*;

import static it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType.EC;
import static it.pagopa.selfcare.onboarding.common.InstitutionType.PA;

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
            case PDND_INFOCAMERE -> Uni.createFrom().item(new WrapperInfocamere(onboarding, infocamerePdndApi));
            case ANAC -> Uni.createFrom().item(new WrapperANAC(onboarding, stationsApi));
            case IVASS -> Uni.createFrom().item(new WrapperIVASS(onboarding, insuranceCompaniesApi));
            default -> getResourceFromIPA(onboarding);
        };
    }

    private Uni<Wrapper<?>> getResourceFromIPA(Onboarding onboarding) {
        return switch ((onboarding.getInstitution().getSubunitType() != null) ? onboarding.getInstitution().getSubunitType() : EC) {
            case AOO -> Uni.createFrom().item(new WrapperAOO(onboarding, aooApi, institutionRegistryProxyApi, uoApi));
            case UO -> Uni.createFrom().item(new WrapperUO(onboarding, institutionRegistryProxyApi, uoApi));
            default -> getResourceFromInstitutionType(onboarding);
        };
    }

    private Uni<Wrapper<?>> getResourceFromInstitutionType(Onboarding onboarding) {
        return switch ((onboarding.getInstitution().getInstitutionType() != null) ? onboarding.getInstitution().getInstitutionType() : PA) {
            case GSP -> Uni.createFrom().item(new WrapperGPS(onboarding, institutionRegistryProxyApi, uoApi));
            case PT -> Uni.createFrom().item(new WrapperPT(onboarding, institutionRegistryProxyApi, uoApi));
            default -> Uni.createFrom().item(new WrapperIPA(onboarding, institutionRegistryProxyApi, uoApi));
        };
    }
}
