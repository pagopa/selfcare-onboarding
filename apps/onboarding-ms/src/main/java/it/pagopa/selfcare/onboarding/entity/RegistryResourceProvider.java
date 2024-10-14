package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.Origin;
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

    public Wrapper<?> create(Onboarding onboarding) {
        //todo add other origins
        return switch (onboarding.getInstitution().getOrigin() != null ? onboarding.getInstitution().getOrigin() : Origin.SELC) {
            case PDND_INFOCAMERE -> new WrapperPDNDInfocamere(onboarding, infocamerePdndApi);
            case ANAC -> new WrapperANAC(onboarding, stationsApi);
            case IVASS -> new WrapperIVASS(onboarding, insuranceCompaniesApi);
            case IPA -> getResourceFromIPA(onboarding);
            default -> new WrapperSELC(onboarding);
        };
    }

    private Wrapper<?> getResourceFromIPA(Onboarding onboarding) {
        return switch ((onboarding.getInstitution().getSubunitType() != null) ? onboarding.getInstitution().getSubunitType() : EC) {
            case AOO -> new WrapperAOO(onboarding, aooApi, uoApi);
            case UO -> new WrapperUO(onboarding, uoApi);
            default -> getResourceFromInstitutionType(onboarding);
        };
    }

    private Wrapper<?> getResourceFromInstitutionType(Onboarding onboarding) {
        return switch ((onboarding.getInstitution().getInstitutionType() != null) ? onboarding.getInstitution().getInstitutionType() : PA) {
            case GSP -> new WrapperGPS(onboarding, uoApi);
            case PT -> new WrapperPT(onboarding, uoApi);
            default -> new WrapperUO(onboarding, uoApi);
        };
    }
}
