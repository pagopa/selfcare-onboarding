package it.pagopa.selfcare.onboarding.entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.entity.registry.RegistryManagerInfocamere;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamereApi;
import org.openapi.quarkus.party_registry_proxy_json.model.BusinessResource;
import org.openapi.quarkus.party_registry_proxy_json.model.BusinessesResource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

@QuarkusTest
public class RegistryManagerInfocamereTest {
    @InjectMock
    @RestClient
    InfocamereApi infocamereApi;

    @InjectMock
    @RestClient
    UserApi userApi;

    @Test
    void retrieveInstitution() {
        Onboarding onboarding = createOnboarding();
        RegistryManagerInfocamere registryManagerInfocamere = new RegistryManagerInfocamere(onboarding, infocamereApi, userApi);
        BusinessResource businessResource = new BusinessResource();
        businessResource.setBusinessTaxId("taxCode");
        BusinessesResource resource = new BusinessesResource();
        resource.setBusinesses(List.of(businessResource));
        mockUserRegistry();
        when(infocamereApi.institutionsByLegalTaxIdUsingPOST(any())).thenReturn(Uni.createFrom().item(resource));
        BusinessesResource result = registryManagerInfocamere.retrieveInstitution();
        assertNotNull(result);
        assertFalse(result.getBusinesses().isEmpty());
        assertEquals("taxCode", result.getBusinesses().get(0).getBusinessTaxId());
    }


    @Test
    void isNotValid() {
        Onboarding onboarding = createOnboarding();
        RegistryManagerInfocamere registryManagerInfocamere = new RegistryManagerInfocamere(onboarding, infocamereApi, userApi);
        BusinessesResource businessesResource = new BusinessesResource();
        businessesResource.setBusinesses(List.of());
        registryManagerInfocamere.setResource(businessesResource);
        UniAssertSubscriber<Boolean> subscriber = registryManagerInfocamere.isValid()
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    private Onboarding createOnboarding() {
        Onboarding onboarding = new Onboarding();
        User user = new User();
        user.setRole(PartyRole.MANAGER);
        user.setId("idManager");
        onboarding.setUsers(List.of(user));
        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
        institution.setOrigin(Origin.INFOCAMERE);
        onboarding.setInstitution(institution);
        return onboarding;
    }

    private void mockUserRegistry() {
        UserResource user = new UserResource();
        user.setFiscalCode("taxCode");
        when(userApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(user));
    }
}
