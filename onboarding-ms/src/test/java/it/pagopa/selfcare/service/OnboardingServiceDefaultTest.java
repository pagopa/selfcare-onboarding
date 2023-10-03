package it.pagopa.selfcare.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.commons.base.utils.InstitutionType;
import it.pagopa.selfcare.controller.request.*;
import it.pagopa.selfcare.controller.response.OnboardingResponse;
import it.pagopa.selfcare.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.repository.OnboardingRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.ProductOperations;
import org.openapi.quarkus.product_json.model.ProductResource;
import org.openapi.quarkus.product_json.model.ProductRole;
import org.openapi.quarkus.product_json.model.ProductRoleInfoRes;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserId;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @InjectMock
    @RestClient
    ProductApi productApi;

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
    void onboardingPa_throwExceptionIfUserFoundedAndProductIsNotValid() {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new InstitutionBaseRequest());

        mockSimpleSearchPOSTAndPersist();

        when(productApi.getProductIsValidUsingGET(onboardingRequest.getProductId()))
                .thenReturn(Uni.createFrom().nullItem());

        onboardingService.onboardingPa(onboardingRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(OnboardingNotAllowedException.class);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(productApi, times(1))
                .getProductIsValidUsingGET(onboardingRequest.getProductId());
        verifyNoMoreInteractions(userRegistryApi);
    }

    @Test
    void onboardingPa_throwExceptionIfUserFoundedAndProductIsNotDelegable() {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        InstitutionBaseRequest institutionBaseRequest = new InstitutionBaseRequest();
        institutionBaseRequest.setInstitutionType(InstitutionType.PT);
        onboardingRequest.setInstitution(institutionBaseRequest);

        mockSimpleSearchPOSTAndPersist();

        ProductResource productResource = new ProductResource();
        productResource.setDelegable(Boolean.FALSE);
        when(productApi.getProductIsValidUsingGET(onboardingRequest.getProductId()))
                .thenReturn(Uni.createFrom().item(productResource));

        onboardingService.onboardingPa(onboardingRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(OnboardingNotAllowedException.class);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(productApi, times(1))
                .getProductIsValidUsingGET(onboardingRequest.getProductId());
        verifyNoMoreInteractions(userRegistryApi);
    }

    @Test
    void onboardingPa_throwExceptionIfUserFoundedAndProductRoleIsNotValid() {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new InstitutionBaseRequest());

        mockSimpleSearchPOSTAndPersist();

        ProductResource productResource = new ProductResource();
        productResource.setRoleMappings(new HashMap<>());
        when(productApi.getProductIsValidUsingGET(onboardingRequest.getProductId()))
                .thenReturn(Uni.createFrom().item(productResource));

        onboardingService.onboardingPa(onboardingRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(OnboardingNotAllowedException.class);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(productApi, times(1))
                .getProductIsValidUsingGET(onboardingRequest.getProductId());
        verifyNoMoreInteractions(userRegistryApi);
    }

    @Test
    void onboardingPa_throwExceptionIfUserFoundedAndProductParentRoleIsNotValid() {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new InstitutionBaseRequest());

        mockSimpleSearchPOSTAndPersist();

        ProductResource productResource = new ProductResource();
        ProductOperations productParent = new ProductOperations();
        productParent.setRoleMappings(new HashMap<>());
        productResource.setProductOperations(productParent);
        when(productApi.getProductIsValidUsingGET(onboardingRequest.getProductId()))
                .thenReturn(Uni.createFrom().item(productResource));

        onboardingService.onboardingPa(onboardingRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(OnboardingNotAllowedException.class);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(productApi, times(1))
                .getProductIsValidUsingGET(onboardingRequest.getProductId());
        verifyNoMoreInteractions(userRegistryApi);
    }

    @Test
    void onboardingSa_whenUserFoundedAndWillNotUpdate() {
        OnboardingSaRequest onboardingRequest = new OnboardingSaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new InstitutionBaseRequest());

        mockSimpleSearchPOSTAndPersist();

        mockSimpleProductValid(onboardingRequest.getProductId());

        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingService.onboardingSa(onboardingRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem();

        OnboardingResponse actual = subscriber.assertCompleted().getItem();
        assertNotNull(actual);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verifyNoMoreInteractions(userRegistryApi);
    }

    void mockSimpleProductValid(String productId) {
        ProductResource productResource = new ProductResource();
        Map<String, ProductRoleInfoRes> roleMapping = new HashMap<>();
        roleMapping.put(manager.getRole().name(), ProductRoleInfoRes.builder()
                .roles(List.of(ProductRole.builder().code("admin").build()))
                .build());
        productResource.setRoleMappings(roleMapping);
        when(productApi.getProductIsValidUsingGET(productId))
                .thenReturn(Uni.createFrom().item(productResource));
    }


    @Test
    void onboardingPsp_whenUserFoundedAndWillNotUpdate() {
        OnboardingPspRequest onboardingRequest = new OnboardingPspRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new InstitutionPspRequest());

        mockSimpleSearchPOSTAndPersist();

        mockSimpleProductValid(onboardingRequest.getProductId());

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
        onboardingDefaultRequest.setProductId("productId");
        onboardingDefaultRequest.setInstitution(new InstitutionBaseRequest());

        mockSimpleSearchPOSTAndPersist();

        mockSimpleProductValid(onboardingDefaultRequest.getProductId());

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

        OnboardingDefaultRequest request = new OnboardingDefaultRequest();
        request.setUsers(List.of(manager));
        request.setProductId("productId");
        request.setInstitution(new InstitutionBaseRequest());

        Mockito.when(userRegistryApi.updateUsingPATCH(any(),any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build()));

        mockSimpleSearchPOSTAndPersist();

        mockSimpleProductValid(request.getProductId());

        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingService.onboarding(request)
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
        OnboardingDefaultRequest request = new OnboardingDefaultRequest();
        request.setUsers(List.of(manager));
        request.setProductId("productId");
        request.setInstitution(new InstitutionBaseRequest());
        final UUID createUserId = UUID.randomUUID();

        Mockito.when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().failure(new WebApplicationException(404)));

        Mockito.when(userRegistryApi.saveUsingPATCH(any()))
                .thenReturn(Uni.createFrom().item(UserId.builder().id(createUserId).build()));

        mockSimpleProductValid(request.getProductId());

        Mockito.when(onboardingRepository.persistOrUpdate(any()))
                .thenAnswer(arg -> Uni.createFrom().item(arg.getArguments()[0]));

        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingService.onboarding(request)
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
