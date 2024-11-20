package it.pagopa.selfcare.onboarding.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.entity.registry.*;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InsuranceCompaniesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.StationsApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;

@QuarkusTest
class RegistryResourceProviderTest {

    @Inject
    RegistryResourceFactory registryResourceProvider;

    @InjectMock
    @RestClient
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionApi;

    @InjectMock
    @RestClient
    UoApi uoApi;

    @InjectMock
    @RestClient
    InsuranceCompaniesApi insuranceCompaniesApi;

    @InjectMock
    @RestClient
    InfocamerePdndApi infocamerePdndApi;

    @InjectMock
    @RestClient
    StationsApi stationsApi;

    private Onboarding createOnboarding(Origin origin) {
        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setOrigin(origin);
        onboarding.setInstitution(institution);
        return onboarding;
    }

    @Test
    void getRegistryIPA() {
        RegistryManager<?> registryManager = registryResourceProvider.create(createOnboarding(Origin.IPA));
        assertTrue(registryManager instanceof RegistryManagerIPA);
    }

    @Test
    void getRegistryUO() {
        Onboarding onboarding = createOnboarding(Origin.IPA);
        onboarding.getInstitution().setSubunitType(InstitutionPaSubunitType.UO);
        RegistryManager<?> registryManager = registryResourceProvider.create(onboarding);
        assertTrue(registryManager instanceof RegistryManagerIPAUo);
    }

    @Test
    void getRegistryAOO() {
        Onboarding onboarding = createOnboarding(Origin.IPA);
        onboarding.getInstitution().setSubunitType(InstitutionPaSubunitType.AOO);
        RegistryManager<?> registryManager = registryResourceProvider.create(onboarding);
        assertTrue(registryManager instanceof RegistryManagerIPAAoo);
    }

    @Test
    void getRegistryIVASS() {
        RegistryManager<?> registryManager = registryResourceProvider.create(createOnboarding(Origin.IVASS));
        assertTrue(registryManager instanceof RegistryManagerIVASS);
    }

    @Test
    void getRegistryANAC() {
        RegistryManager<?> registryManager = registryResourceProvider.create(createOnboarding(Origin.ANAC));
        assertTrue(registryManager instanceof RegistryManagerANAC);
    }

    @Test
    void getRegistryPDNDInfocamere() {
        RegistryManager<?> registryManager = registryResourceProvider.create(createOnboarding(Origin.PDND_INFOCAMERE));
        assertTrue(registryManager instanceof RegistryManagerPDNDInfocamere);
    }

    @Test
    void getRegistryInfocamere() {
        RegistryManager<?> registryManager = registryResourceProvider.create(createOnboarding(Origin.INFOCAMERE));
        assertTrue(registryManager instanceof RegistryManagerInfocamere);
    }

    @Test
    void getRegistryADE() {
        RegistryManager<?> registryManager = registryResourceProvider.create(createOnboarding(Origin.ADE));
        assertTrue(registryManager instanceof RegistryManagerADE);
    }

}
