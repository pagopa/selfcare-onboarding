package it.pagopa.selfcare.onboarding.entity;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.Origin;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class RegistryResourceProviderTest {

    @Inject
    RegistryResourceProvider registryResourceProvider;

    @InjectMock
    @RestClient
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionApi;

    @InjectMock
    @RestClient
    UoApi uoApi;

    private Onboarding createOnboarding(Origin origin) {
        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setOrigin(origin);
        onboarding.setInstitution(institution);
        return onboarding;
    }

    @Test
    void getWrapperIPA() {

        when(institutionApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(new InstitutionResource()));

        UniAssertSubscriber<Wrapper<?>> subscriber = registryResourceProvider.getResource(createOnboarding(Origin.IPA))
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        assertTrue(subscriber.getItem() instanceof WrapperIPA);

    }

    @Test
    void getWrapperUO() {

        InstitutionResource institutionResource = new InstitutionResource();
        when(institutionApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(institutionResource));
        when(uoApi.findByUnicodeUsingGET1(any(), any())).thenReturn(Uni.createFrom().item(new UOResource()));

        Onboarding onboarding = createOnboarding(Origin.IPA);
        onboarding.getInstitution().setSubunitType(InstitutionPaSubunitType.UO);
        UniAssertSubscriber<Wrapper<?>> subscriber = registryResourceProvider.getResource(onboarding)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        assertTrue(subscriber.getItem() instanceof WrapperUO);

    }

}
