package it.pagopa.selfcare.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.controller.request.UserRequest;
import it.pagopa.selfcare.controller.response.OnboardingResponse;
import it.pagopa.selfcare.repository.OnboardingRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserId;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.util.List;
import java.util.UUID;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@QuarkusTest
public class OnboardingServiceDefaultTest {

    @Inject
    OnboardingServiceDefault onboardingService;

    @InjectMock
    OnboardingRepository onboardingRepository;

    @InjectMock
    @RestClient
    UserApi userRegistryApi;

    final static UserRequest manager = UserRequest.builder()
            .name("name")
            .surname("surname")
            .taxCode("taxCode")
            .role(PartyRole.MANAGER)
            .build();

    final static UserResource managerResource;

    static {
        managerResource = new UserResource();
        managerResource.setId(UUID.randomUUID());
        managerResource.setName(new CertifiableFieldResourceOfstring()
                .value(manager.getName())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        managerResource.setFamilyName(new CertifiableFieldResourceOfstring()
                .value(manager.getSurname())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
    }

    @Test
    void onboarding_shouldThrowExceptionIfRoleNotValid() {
        OnboardingDefaultRequest onboardingDefaultRequest = new OnboardingDefaultRequest();
        onboardingDefaultRequest.setUsers(List.of(UserRequest.builder()
                .taxCode("taxCode")
                .role(PartyRole.OPERATOR)
                .build()));

        onboardingService.onboarding(onboardingDefaultRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).assertFailed();
    }

    void mockSimpleSearchPOSTAndPersist(){

        Mockito.when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource));

        Mockito.when(onboardingRepository.persistOrUpdate(any()))
                .thenAnswer(arg -> Uni.createFrom().item(arg.getArguments()[0]));
    }
    @Test
    void onboardingPa_whenUserFoundedAndWillNotUpdate() {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));

        mockSimpleSearchPOSTAndPersist();

        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingService.onboardingPa(onboardingRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem();

        OnboardingResponse actual = subscriber.assertCompleted().getItem();
        assertNotNull(actual);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verifyNoMoreInteractions(userRegistryApi);
    }


    @Test
    void onboardingPsp_whenUserFoundedAndWillNotUpdate() {
        OnboardingPspRequest onboardingRequest = new OnboardingPspRequest();
        onboardingRequest.setUsers(List.of(manager));

        mockSimpleSearchPOSTAndPersist();

        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingService.onboardingPsp(onboardingRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem();

        OnboardingResponse actual = subscriber.assertCompleted().getItem();
        assertNotNull(actual);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verifyNoMoreInteractions(userRegistryApi);
    }


    @Test
    void onboarding_whenUserFoundedAndWillNotUpdate() {
        OnboardingDefaultRequest onboardingDefaultRequest = new OnboardingDefaultRequest();
        onboardingDefaultRequest.setUsers(List.of(manager));

        mockSimpleSearchPOSTAndPersist();

        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingService.onboarding(onboardingDefaultRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem();

        OnboardingResponse actual = subscriber.assertCompleted().getItem();
        assertNotNull(actual);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verifyNoMoreInteractions(userRegistryApi);
    }

    @Test
    void onboarding_whenUserFoundedAndWillUpdate() {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        OnboardingDefaultRequest onboardingDefaultRequest = new OnboardingDefaultRequest();
        onboardingDefaultRequest.setUsers(List.of(manager));

        Mockito.when(userRegistryApi.updateUsingPATCH(any(),any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build()));

        mockSimpleSearchPOSTAndPersist();

        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingService.onboarding(onboardingDefaultRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem();

        OnboardingResponse actual = subscriber.assertCompleted().getItem();
        assertNotNull(actual);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(userRegistryApi, times(1))
                .updateUsingPATCH(any(),any());
        verifyNoMoreInteractions(userRegistryApi);
    }

    @Test
    void onboarding_whenUserNotFoundedAndWillSave() {
        OnboardingDefaultRequest onboardingDefaultRequest = new OnboardingDefaultRequest();
        onboardingDefaultRequest.setUsers(List.of(manager));
        final UUID createUserId = UUID.randomUUID();

        Mockito.when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().failure(new WebApplicationException(404)));

        Mockito.when(userRegistryApi.saveUsingPATCH(any()))
                .thenReturn(Uni.createFrom().item(UserId.builder().id(createUserId).build()));

        Mockito.when(onboardingRepository.persistOrUpdate(any()))
                .thenAnswer(arg -> Uni.createFrom().item(arg.getArguments()[0]));

        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingService.onboarding(onboardingDefaultRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem();

        OnboardingResponse actual = subscriber.assertCompleted().getItem();
        assertNotNull(actual);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(userRegistryApi, times(1))
                .saveUsingPATCH(any());
        verifyNoMoreInteractions(userRegistryApi);
    }


    @Test
    void onboarding_shouldThrowExceptionIfUserRegistryFails() {
        OnboardingDefaultRequest onboardingDefaultRequest = new OnboardingDefaultRequest();
        onboardingDefaultRequest.setUsers(List.of(manager));

        Mockito.when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().failure(new WebApplicationException()));

        onboardingService.onboarding(onboardingDefaultRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).awaitFailure();

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verifyNoMoreInteractions(userRegistryApi);
    }
}
