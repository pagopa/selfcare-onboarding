package it.pagopa.selfcare.onboarding.service;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.util.InstitutionPaSubunitType;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.core_json.api.OnboardingApi;
import org.openapi.quarkus.onboarding_functions_json.api.OrchestrationApi;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserId;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_INTEROP;
import static it.pagopa.selfcare.onboarding.service.OnboardingServiceDefault.USERS_FIELD_TAXCODE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class OnboardingServiceDefaultTest {

    @Inject
    OnboardingServiceDefault onboardingService;

    @InjectMock
    @RestClient
    UserApi userRegistryApi;

    @InjectMock
    ProductService productService;

    @InjectMock
    @RestClient
    AooApi aooApi;

    @InjectMock
    @RestClient
    UoApi uoApi;


    @InjectMock
    AzureBlobClient azureBlobClient;

    @InjectMock
    SignatureService signatureService;

    @InjectMock
    @RestClient
    OnboardingApi onboardingApi;

    @InjectMock
    @RestClient
    OrchestrationApi orchestrationApi;

    final static UserRequest manager = UserRequest.builder()
            .name("name")
            .surname("surname")
            .taxCode("taxCode")
            .role(PartyRole.MANAGER)
            .build();

    final static UserResource managerResource;

    final static File testFile = new File("src/test/resources/application.properties");

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

    void mockPersistOnboarding(UniAsserter asserter) {
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.persist(any(Onboarding.class), any()))
                .thenAnswer(arg -> {
                    Onboarding onboarding = (Onboarding) arg.getArguments()[0];
                    onboarding.setId(ObjectId.get());
                    return Uni.createFrom().nullItem();
                }));
    }

    @Test
    @RunOnVertxContext
    void onboarding_shouldThrowExceptionIfRoleNotValid(UniAsserter asserter) {
        OnboardingDefaultRequest onboardingDefaultRequest = new OnboardingDefaultRequest();
        onboardingDefaultRequest.setInstitution(new InstitutionBaseRequest());
        onboardingDefaultRequest.setUsers(List.of(UserRequest.builder()
                .taxCode("taxCode")
                .role(PartyRole.OPERATOR)
                .build()));

        mockPersistOnboarding(asserter);

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingDefaultRequest),
                InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void onboardingPa_throwExceptionIfUserFoundedAndProductThrowException(UniAsserter asserter) {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        UserRequest manager = UserRequest.builder()
                .name("currentName")
                .surname("currentSurname")
                .taxCode("currentTaxCode")
                .role(PartyRole.MANAGER)
                .build();

        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new InstitutionBaseRequest());

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenThrow(IllegalArgumentException.class));

        asserter.assertFailedWith(() -> onboardingService.onboardingPa(onboardingRequest), OnboardingNotAllowedException.class);
    }


    @Test
    @RunOnVertxContext
    void onboarding_throwExceptionIfUserFoundedAndProductIsNotValid(UniAsserter asserter) {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new InstitutionBaseRequest());

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(null));

        asserter.assertFailedWith(() -> onboardingService.onboardingPa(onboardingRequest), OnboardingNotAllowedException.class);

        //asserter.execute(() -> verify(userRegistryApi, times(1))
                //.searchUsingPOST(any(),any()));
        //asserter.execute(() -> verify(productService, times(1))
                //.getProductIsValid(onboardingRequest.getProductId()));
        //asserter.execute(() -> verifyNoMoreInteractions(userRegistryApi));
    }

    @Test
    @RunOnVertxContext
    void onboarding_throwExceptionIfUserFoundedAndProductIsNotDelegable(UniAsserter asserter) {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        InstitutionBaseRequest institutionBaseRequest = new InstitutionBaseRequest();
        institutionBaseRequest.setInstitutionType(InstitutionType.PT);
        onboardingRequest.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        Product productResource = new Product();
        productResource.setDelegable(Boolean.FALSE);
        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource));

        asserter.assertFailedWith(() -> onboardingService.onboardingPa(onboardingRequest), OnboardingNotAllowedException.class);

        /*verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(productService, times(1))
                .getProductIsValid(onboardingRequest.getProductId());
        verifyNoMoreInteractions(userRegistryApi);*/
    }

    @Test
    @RunOnVertxContext
    void onboarding_throwExceptionIfUserFoundedAndProductRoleIsNotValid(UniAsserter asserter) {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        InstitutionBaseRequest institutionBaseRequest = new InstitutionBaseRequest();
        institutionBaseRequest.setInstitutionType(InstitutionType.PG);
        onboardingRequest.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        Product productResource = new Product();
        productResource.setRoleMappings(new HashMap<>());
        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource));

        asserter.assertFailedWith(() -> onboardingService.onboardingPa(onboardingRequest), OnboardingNotAllowedException.class);

        /*verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(productService, times(1))
                .getProductIsValid(onboardingRequest.getProductId());
        verifyNoMoreInteractions(userRegistryApi);*/
    }

    @Test
    @RunOnVertxContext
    void onboardingPa_throwExceptionIfUserFoundedAndProductParentRoleIsNotValid(UniAsserter asserter) {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new InstitutionBaseRequest());

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        Product productResource = new Product();
        Product productParent = new Product();
        productParent.setRoleMappings(new HashMap<>());
        productResource.setParent(productParent);
        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource));

        asserter.assertFailedWith(() -> onboardingService.onboardingPa(onboardingRequest), OnboardingNotAllowedException.class);

        /*verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(productService, times(1))
                .getProductIsValid(onboardingRequest.getProductId());
        verifyNoMoreInteractions(userRegistryApi)*/;
    }

    @Test
    @RunOnVertxContext
    void onboardingPa_throwExceptionIfProductAlreadyOnboarded(UniAsserter asserter) {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        InstitutionBaseRequest institutionBaseRequest = new InstitutionBaseRequest();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        onboardingRequest.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource)));


        Product productResource = createDummyProduct(onboardingRequest.getProductId(),false);

        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource));

        asserter.execute(() -> when(onboardingApi.verifyOnboardingInfoUsingHEAD(institutionBaseRequest.getTaxCode(), onboardingRequest.getProductId(), institutionBaseRequest.getSubunitCode()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        asserter.assertFailedWith(() -> onboardingService.onboardingPa(onboardingRequest), InvalidRequestException.class);

        /*verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(onboardingApi, times(1))
                .verifyOnboardingInfoUsingHEAD(institutionBaseRequest.getTaxCode(), onboardingRequest.getProductId(), institutionBaseRequest.getSubunitCode());
        verifyNoMoreInteractions(userRegistryApi);*/
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescritpionForAooOrUo_Aoo(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        OnboardingDefaultRequest request = new OnboardingDefaultRequest();
        request.setUsers(List.of(manager));
        request.setProductId(PROD_INTEROP.getValue());
        InstitutionBaseRequest institutionBaseRequest = new InstitutionBaseRequest();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.AOO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(),any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        AOOResource aooResource = new AOOResource();
        aooResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(aooApi.findByUnicodeUsingGET(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().item(aooResource)));

        asserter.assertThat(() -> onboardingService.onboarding(request), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescritpionForAooOrUo_AooNotFound(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        OnboardingDefaultRequest request = new OnboardingDefaultRequest();
        request.setUsers(List.of(manager));
        request.setProductId(PROD_INTEROP.getValue());
        InstitutionBaseRequest institutionBaseRequest = new InstitutionBaseRequest();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.AOO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(),any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        AOOResource aooResource = new AOOResource();
        aooResource.setDenominazioneEnte("TEST");
        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(exception.getResponse()).thenReturn(response);
        asserter.execute(() -> when(aooApi.findByUnicodeUsingGET(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().failure(exception)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request), ResourceNotFoundException.class);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescritpionForAooOrUo_AooException(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        OnboardingDefaultRequest request = new OnboardingDefaultRequest();
        request.setUsers(List.of(manager));
        request.setProductId(PROD_INTEROP.getValue());
        InstitutionBaseRequest institutionBaseRequest = new InstitutionBaseRequest();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.AOO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(),any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        AOOResource aooResource = new AOOResource();
        aooResource.setDenominazioneEnte("TEST");
        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(exception.getResponse()).thenReturn(response);
        asserter.execute(() -> when(aooApi.findByUnicodeUsingGET(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().failure(exception)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request), WebApplicationException.class);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescritpionForAooOrUo_Uo(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        OnboardingDefaultRequest request = new OnboardingDefaultRequest();
        request.setUsers(List.of(manager));
        request.setProductId(PROD_INTEROP.getValue());
        InstitutionBaseRequest institutionBaseRequest = new InstitutionBaseRequest();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.UO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(),any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().item(uoResource)));

        asserter.assertThat(() -> onboardingService.onboarding(request), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescritpionForAooOrUo_UoNotFound(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        OnboardingDefaultRequest request = new OnboardingDefaultRequest();
        request.setUsers(List.of(manager));
        request.setProductId(PROD_INTEROP.getValue());
        InstitutionBaseRequest institutionBaseRequest = new InstitutionBaseRequest();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.UO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(),any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().item(uoResource)));

        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(exception.getResponse()).thenReturn(response);

        asserter.assertFailedWith(() -> onboardingService.onboarding(request), ResourceNotFoundException.class);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescritpionForAooOrUo_UoException(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        OnboardingDefaultRequest request = new OnboardingDefaultRequest();
        request.setUsers(List.of(manager));
        request.setProductId(PROD_INTEROP.getValue());
        InstitutionBaseRequest institutionBaseRequest = new InstitutionBaseRequest();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.UO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(),any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().item(uoResource)));

        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(exception.getResponse()).thenReturn(response);

        asserter.assertFailedWith(() -> onboardingService.onboarding(request), WebApplicationException.class);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    void mockSimpleSearchPOSTAndPersist(UniAsserter asserter){

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        asserter.execute(() -> when(Onboarding.persistOrUpdate(any(List.class)))
                .thenAnswer(arg -> {
                    List<Onboarding> onboardings = (List<Onboarding>) arg.getArguments()[0];
                    onboardings.get(0).setId(ObjectId.get());
                    return Uni.createFrom().nullItem();
                }));

        asserter.execute(() -> when(orchestrationApi.apiStartOnboardingOrchestrationGet(any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse())));
    }
    @Test
    @RunOnVertxContext
    void onboardingSa_whenUserFoundedAndWillNotUpdate(UniAsserter asserter) {
        OnboardingSaRequest onboardingRequest = new OnboardingSaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        InstitutionBaseRequest institutionBaseRequest = new InstitutionBaseRequest();
        institutionBaseRequest.setInstitutionType(InstitutionType.SA);
        onboardingRequest.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.onboardingSa(onboardingRequest), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    void mockSimpleProductValidAssert(String productId, boolean hasParent, UniAsserter asserter) {
        Product productResource = createDummyProduct(productId,hasParent);

        asserter.execute(() -> when(productService.getProductIsValid(productId))
                .thenReturn(productResource));
    }

    Product createDummyProduct(String productId, boolean hasParent) {
        Product productResource = new Product();
        productResource.setId(productId);
        Map<PartyRole, ProductRoleInfo> roleMapping = new HashMap<>();
        ProductRole productRole = new ProductRole();
        productRole.setCode("admin");
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setRoles(List.of(productRole));
        roleMapping.put(manager.getRole(), productRoleInfo);
        productResource.setRoleMappings(roleMapping);

        if(hasParent) {
            Product parent = new Product();
            parent.setId("productParentId");
            Map<PartyRole, ProductRoleInfo> roleParentMapping = new HashMap<>();
            roleParentMapping.put(manager.getRole(), productRoleInfo);
            parent.setRoleMappings(roleParentMapping);

            productResource.setParent(parent);
        }

        return productResource;
    }


    @Test
    @RunOnVertxContext
    void onboardingPsp_whenUserFoundedAndWillNotUpdate(UniAsserter asserter) {
        OnboardingPspRequest onboardingRequest = new OnboardingPspRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        InstitutionPspRequest institutionPspRequest = new InstitutionPspRequest();
        institutionPspRequest.setInstitutionType(InstitutionType.PSP);
        onboardingRequest.setInstitution(institutionPspRequest);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.onboardingPsp(onboardingRequest), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    @Test
    @RunOnVertxContext
    void onboardingPsp_whenUserFoundedAndWillNotUpdateAndProductHasParent(UniAsserter asserter) {
        OnboardingPspRequest onboardingRequest = new OnboardingPspRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        InstitutionPspRequest institutionPspRequest = new InstitutionPspRequest();
        institutionPspRequest.setInstitutionType(InstitutionType.PSP);
        onboardingRequest.setInstitution(institutionPspRequest);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), true, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.onboardingPsp(onboardingRequest), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    @Test
    @RunOnVertxContext
    void onboarding_whenUserFoundedAndWillNotUpdate(UniAsserter asserter) {
        OnboardingDefaultRequest onboardingDefaultRequest = new OnboardingDefaultRequest();
        onboardingDefaultRequest.setUsers(List.of(manager));
        onboardingDefaultRequest.setProductId("productId");
        onboardingDefaultRequest.setInstitution(new InstitutionBaseRequest());

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingDefaultRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.onboarding(onboardingDefaultRequest), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenUserFoundedAndWillUpdate(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        OnboardingDefaultRequest request = new OnboardingDefaultRequest();
        request.setUsers(List.of(manager));
        request.setProductId(PROD_INTEROP.getValue());
        InstitutionPspRequest institutionPspRequest = new InstitutionPspRequest();
        institutionPspRequest.setInstitutionType(InstitutionType.GSP);
        request.setInstitution(institutionPspRequest);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(),any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.onboarding(request), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenUserNotFoundedAndWillSave(UniAsserter asserter) {
        OnboardingDefaultRequest request = new OnboardingDefaultRequest();
        request.setUsers(List.of(manager));
        request.setProductId("productId");
        request.setInstitution(new InstitutionBaseRequest());
        final UUID createUserId = UUID.randomUUID();

        mockPersistOnboarding(asserter);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().failure(new WebApplicationException(404))));

        asserter.execute(() -> when(userRegistryApi.saveUsingPATCH(any()))
                .thenReturn(Uni.createFrom().item(UserId.builder().id(createUserId).build())));

        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.execute(() -> when(Onboarding.persistOrUpdate(any(List.class)))
                .thenAnswer(arg -> {
                    List<Onboarding> onboardings = (List<Onboarding>) arg.getArguments()[0];
                    onboardings.get(0).setId(ObjectId.get());
                    return Uni.createFrom().nullItem();
                }));

        asserter.assertThat(() -> onboardingService.onboarding(request), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    @Test
    @RunOnVertxContext
    void onboarding_shouldThrowExceptionIfUserRegistryFails(UniAsserter asserter) {
        OnboardingDefaultRequest onboardingDefaultRequest = new OnboardingDefaultRequest();
        onboardingDefaultRequest.setInstitution(new InstitutionBaseRequest());
        onboardingDefaultRequest.setUsers(List.of(manager));

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().failure(new WebApplicationException())));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingDefaultRequest), WebApplicationException.class);

        /*verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verifyNoMoreInteractions(userRegistryApi);*/
    }

    void mockVerifyOnboardingNotFound(UniAsserter asserter){
        asserter.execute(() -> when(onboardingApi.verifyOnboardingInfoUsingHEAD(any(), any(), any()))
                .thenReturn(Uni.createFrom().failure(new ClientWebApplicationException(404))));
    }

    @Test
    @RunOnVertxContext
    void completeWithoutSignatureVerification_shouldThrowExceptionWhenExpired(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setExpiringDate(LocalDateTime.now().minusDays(1));
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        asserter.assertFailedWith(() -> onboardingService.completeWithoutSignatureVerification(onboarding.getId().toHexString(), null),
                InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void completeWithoutSignatureVerification(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter, onboarding.getId().toHexString());

        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.completeWithoutSignatureVerification(onboarding.getId().toHexString(), testFile),
                Assertions::assertNotNull);

    }

    @Test
    @RunOnVertxContext
    void complete_shouldThrowExceptionWhenSignatureFail(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter, onboarding.getId().toHexString());

        //Mock find manager fiscal code
        String actualUseUid = onboarding.getUsers().get(0).getId();
        UserResource actualUserResource = new UserResource();
        actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
        asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                .thenReturn(Uni.createFrom().item(actualUserResource)));

        //Mock contract signature fail
        asserter.execute(() -> doThrow(InvalidRequestException.class)
                .when(signatureService)
                .verifySignature(any(),any(),any()));

        asserter.assertFailedWith(() -> onboardingService.complete(onboarding.getId().toHexString(), testFile),
                InvalidRequestException.class);
    }


    @Test
    @RunOnVertxContext
    void complete(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter, onboarding.getId().toHexString());

        //Mock find manager fiscal code
        String actualUseUid = onboarding.getUsers().get(0).getId();
        UserResource actualUserResource = new UserResource();
        actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
        asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                .thenReturn(Uni.createFrom().item(actualUserResource)));

        //Mock contract signature fail
        asserter.execute(() -> doNothing()
                .when(signatureService)
                .verifySignature(any(),any(),any()));

        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        when(azureBlobClient.uploadFile(any(),any(),any())).thenReturn("upload-file");

        asserter.assertThat(() -> onboardingService.complete(onboarding.getId().toHexString(), testFile),
                Assertions::assertNotNull);
    }

    private void mockFindToken(UniAsserter asserter, String onboardingId) {
        Token token = new Token();
        token.setChecksum("actual-checksum");
        asserter.execute(() -> PanacheMock.mock(Token.class));
        asserter.execute(() -> when(Token.list("onboardingId", onboardingId))
                .thenReturn(Uni.createFrom().item(List.of(token))));
    }

    private Onboarding createDummyOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(ObjectId.get());
        onboarding.setProductId("prod-id");
        onboarding.setExpiringDate(LocalDateTime.now().plusDays(1));

        Institution institution = new Institution();
        onboarding.setInstitution(institution);

        User user = new User();
        user.setId("actual-user-id");
        user.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(user));
        return onboarding;
    }

}
