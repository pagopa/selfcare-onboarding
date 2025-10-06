package it.pagopa.selfcare.onboarding.entity;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.entity.registry.RegistryManagerPDNDInfocamere;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.model.PDNDBusinessResource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.SaveUserDto;
import org.openapi.quarkus.user_registry_json.model.UserId;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.UserSearchDto;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
class RegistryManagerPDNDInfocamereTest {

    @InjectMock
    @RestClient
    UserApi userRegistryApi;

    @InjectMock
    @RestClient
    InfocamerePdndApi infocamerePdndApi;

    private RegistryManagerPDNDInfocamere manager;
    private Onboarding baseOnboarding;

    @BeforeEach
    void setUp() {
        reset(userRegistryApi, infocamerePdndApi);

        baseOnboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PRV_PF);
        institution.setTaxCode("RSSMRA80A01H501U");
        institution.setDigitalAddress("test@pec.it");
        institution.setDescription("Test Business Name");
        baseOnboarding.setInstitution(institution);

        manager = new RegistryManagerPDNDInfocamere(baseOnboarding, infocamerePdndApi, userRegistryApi);

        PDNDBusinessResource resource = new PDNDBusinessResource();
        resource.setDigitalAddress("test@pec.it");
        resource.setBusinessName("Test Business Name");
        manager.setRegistryResource(resource);
    }

    @Test
    void customValidation_shouldUpdateOnboarding_whenUserExists() {
        UUID existingUserId = UUID.randomUUID();
        UserResource existingUser = new UserResource();
        existingUser.setId(existingUserId);

        when(userRegistryApi.searchUsingPOST(anyString(), any(UserSearchDto.class)))
                .thenReturn(Uni.createFrom().item(existingUser));

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Onboarding result = subscriber.awaitItem().getItem();
        assertEquals(existingUserId.toString(), result.getInstitution().getTaxCode());
        assertEquals(existingUserId.toString(), result.getInstitution().getOriginId());

        verify(userRegistryApi, never()).saveUsingPATCH(any());
        verify(userRegistryApi, times(1)).searchUsingPOST(any(), any());
    }

    @Test
    void customValidation_shouldCreateUser_whenUserNotFound404() {
        UUID newUserIdValue = UUID.randomUUID();
        UserId userIdResponse = new UserId();
        userIdResponse.setId(newUserIdValue);

        WebApplicationException notFoundException = new WebApplicationException(Response.Status.NOT_FOUND);
        when(userRegistryApi.searchUsingPOST(anyString(), any(UserSearchDto.class)))
                .thenReturn(Uni.createFrom().failure(notFoundException));

        when(userRegistryApi.saveUsingPATCH(any(SaveUserDto.class)))
                .thenReturn(Uni.createFrom().item(userIdResponse));

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Onboarding result = subscriber.awaitItem().getItem();
        assertEquals(newUserIdValue.toString(), result.getInstitution().getTaxCode());
        assertEquals(newUserIdValue.toString(), result.getInstitution().getOriginId());

        ArgumentCaptor<SaveUserDto> captor = ArgumentCaptor.forClass(SaveUserDto.class);
        verify(userRegistryApi, times(1)).saveUsingPATCH(captor.capture());
        assertNotEquals(baseOnboarding.getInstitution().getTaxCode(), captor.getValue().getFiscalCode());
    }

    @Test
    void customValidation_shouldFail_whenSearchFailsWithNon404Error() {
        WebApplicationException serverErrorException = new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        when(userRegistryApi.searchUsingPOST(anyString(), any(UserSearchDto.class)))
                .thenReturn(Uni.createFrom().failure(serverErrorException));

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure();
        verify(userRegistryApi, never()).saveUsingPATCH(any());
    }

    @Test
    void customValidation_shouldDoNothing_whenInstitutionTypeIsNotPrvPf() {

        baseOnboarding.getInstitution().setInstitutionType(InstitutionType.GSP);

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        Onboarding result = subscriber.awaitItem().getItem();

        assertSame(baseOnboarding, result);
        assertEquals("RSSMRA80A01H501U", result.getInstitution().getTaxCode());

        verifyNoInteractions(userRegistryApi);
    }

    @Test
    void isValid_shouldReturnTrue_whenDataMatches() {
        UniAssertSubscriber<Boolean> subscriber = manager.isValid()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        assertTrue(subscriber.awaitItem().getItem());
    }

    @Test
    void isValid_shouldFail_whenDataDoesNotMatch() {
        manager.getRegistryResource().setDigitalAddress("another@pec.it");

        UniAssertSubscriber<Boolean> subscriber = manager.isValid()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Throwable failure = subscriber.awaitFailure().getFailure();
        assertInstanceOf(InvalidRequestException.class, failure);
        assertEquals("Field digitalAddress or description are not valid", failure.getMessage());
    }
}