package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingImportContract;
import it.pagopa.selfcare.onboarding.controller.request.UserRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.controller.response.UserResponse;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceConflictException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapperImpl;
import it.pagopa.selfcare.onboarding.util.InstitutionPaSubunitType;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.openapi.quarkus.core_json.api.OnboardingApi;
import org.openapi.quarkus.onboarding_functions_json.api.OrchestrationApi;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserId;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_INTEROP;
import static it.pagopa.selfcare.onboarding.service.OnboardingServiceDefault.USERS_FIELD_TAXCODE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class OnboardingServiceDefaultTest {

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
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionRegistryProxyApi;

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

    @Spy
    OnboardingMapper onboardingMapper = new OnboardingMapperImpl();

    final static UserRequest manager = UserRequest.builder()
            .name("name")
            .surname("surname")
            .taxCode("taxCode")
            .role(PartyRole.MANAGER)
            .build();

    final static UserResource managerResource;
    final static UserResource managerResourceWk;

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

        managerResourceWk = new UserResource();
        managerResourceWk.setId(UUID.randomUUID());
        managerResourceWk.setName(new CertifiableFieldResourceOfstring()
                .value(manager.getName())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        managerResourceWk.setFamilyName(new CertifiableFieldResourceOfstring()
                .value(manager.getSurname())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));

        Map<String, WorkContactResource> map = new HashMap<>();
        WorkContactResource workContactResource = new WorkContactResource();
        workContactResource.setEmail(new CertifiableFieldResourceOfstring()
                .value("mail@live.it")
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        map.put(UUID.randomUUID().toString(), workContactResource);
        managerResourceWk.setWorkContacts(map);
    }

    @Test
    @RunOnVertxContext
    void onboardingPa_throwExceptionIfProductThrowException(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new Institution());

        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenThrow(IllegalArgumentException.class));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users), OnboardingNotAllowedException.class);
    }


    @Test
    @RunOnVertxContext
    void onboarding_throwExceptionIfProductIsNotValid(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new Institution());

        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenThrow(ProductNotFoundException.class));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users), OnboardingNotAllowedException.class);
    }

    @Test
    @RunOnVertxContext
    void onboardingPa_throwExceptionIfProductAlreadyOnboarded(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        onboardingRequest.setInstitution(institutionBaseRequest);

        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter);

        asserter.execute(() -> when(onboardingApi.verifyOnboardingInfoUsingHEAD(institutionBaseRequest.getTaxCode(), onboardingRequest.getProductId(), institutionBaseRequest.getSubunitCode()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users), ResourceConflictException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_throwExceptionIfProductIsNotDelegable(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PT);
        onboardingRequest.setInstitution(institutionBaseRequest);

        Product productResource = new Product();
        productResource.setDelegable(Boolean.FALSE);
        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource));

        mockVerifyOnboardingNotFound(asserter);

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users), OnboardingNotAllowedException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_throwExceptionIfProductRoleIsNotValid(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PG);
        onboardingRequest.setInstitution(institutionBaseRequest);

        Product productResource = new Product();
        productResource.setRoleMappings(new HashMap<>());
        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource));

        mockVerifyOnboardingNotFound(asserter);
        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users), OnboardingNotAllowedException.class);
    }

    @Test
    @RunOnVertxContext
    void onboardingPa_throwExceptionIfProductParentRoleIsNotValid(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new Institution());

        Product productResource = new Product();
        Product productParent = new Product();
        productParent.setRoleMappings(new HashMap<>());
        productResource.setParent(productParent);

        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource));

        mockVerifyOnboardingNotFound(asserter);
        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users), OnboardingNotAllowedException.class);
    }



    @Test
    @RunOnVertxContext
    void onboarding_shouldThrowExceptionIfRoleNotValid(UniAsserter asserter) {
        Onboarding onboardingDefaultRequest = new Onboarding();
        onboardingDefaultRequest.setInstitution(new Institution());
        onboardingDefaultRequest.setProductId(PROD_INTEROP.getValue());

        List<UserRequest> users = List.of(UserRequest.builder()
                .taxCode("taxCode")
                .role(PartyRole.OPERATOR)
                .build());

        mockSimpleProductValidAssert(onboardingDefaultRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);
        mockPersistOnboarding(asserter);

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingDefaultRequest, users),
                InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_Aoo(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
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

        asserter.assertThat(() -> onboardingService.onboarding(request, users), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_AooNotFound(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.AOO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

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

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users), ResourceNotFoundException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_AooException(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.AOO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

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

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users), WebApplicationException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_Uo(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
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

        asserter.assertThat(() -> onboardingService.onboarding(request, users), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_UoNotFound(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.UO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(exception.getResponse()).thenReturn(response);

        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().failure(exception)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users), ResourceNotFoundException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_UoException(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.UO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(exception.getResponse()).thenReturn(response);

        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().failure(exception)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users), WebApplicationException.class);
    }

    void mockSimpleSearchPOSTAndPersist(UniAsserter asserter){

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        asserter.execute(() -> when(Onboarding.persistOrUpdate(any(List.class)))
                .thenAnswer(arg -> {
                    List<Onboarding> onboardings = (List<Onboarding>) arg.getArguments()[0];
                    onboardings.get(0).setId(UUID.randomUUID().toString());
                    return Uni.createFrom().nullItem();
                }));

        asserter.execute(() -> when(orchestrationApi.apiStartOnboardingOrchestrationGet(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse())));
    }
    @Test
    @RunOnVertxContext
    void onboardingSa_whenUserFoundedAndWillNotUpdate(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.SA);
        onboardingRequest.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.onboarding(onboardingRequest, users), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    Product mockSimpleProductValidAssert(String productId, boolean hasParent, UniAsserter asserter) {
        Product productResource = createDummyProduct(productId,hasParent);
        asserter.execute(() -> when(productService.getProductIsValid(productId))
                .thenReturn(productResource));
        return productResource;
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

            productResource.setParentId(parent.getId());
            productResource.setParent(parent);
        }

        return productResource;
    }


    @Test
    @RunOnVertxContext
    void onboardingPsp_whenUserFoundedAndWillNotUpdate(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionPspRequest = new Institution();
        institutionPspRequest.setInstitutionType(InstitutionType.PSP);
        onboardingRequest.setInstitution(institutionPspRequest);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.onboarding(onboardingRequest, users), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    @Test
    @RunOnVertxContext
    void onboardingPsp_whenUserFoundedAndWillNotUpdateAndProductHasParent(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionPspRequest = new Institution();
        institutionPspRequest.setInstitutionType(InstitutionType.PSP);
        institutionPspRequest.setTaxCode("taxCode");
        onboardingRequest.setInstitution(institutionPspRequest);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        Product product = mockSimpleProductValidAssert(onboardingRequest.getProductId(), true, asserter);

        // mock parent has already onboarding
        asserter.execute(() -> when(onboardingApi.verifyOnboardingInfoUsingHEAD(institutionPspRequest.getTaxCode(), product.getId(), null))
                .thenReturn(Uni.createFrom().failure(new ClientWebApplicationException(404))));
        asserter.execute(() -> when(onboardingApi.verifyOnboardingInfoUsingHEAD(institutionPspRequest.getTaxCode(), product.getParentId(), null))
                .thenReturn(Uni.createFrom().failure(new ResourceConflictException("", ""))));

        asserter.assertThat(() -> onboardingService.onboarding(onboardingRequest, users), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    @Test
    @RunOnVertxContext
    void onboarding_whenUserFoundedAndWillNotUpdate(UniAsserter asserter) {
        Onboarding onboardingDefaultRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingDefaultRequest.setProductId("productId");
        onboardingDefaultRequest.setInstitution(new Institution());

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingDefaultRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.onboarding(onboardingDefaultRequest, users), Assertions::assertNotNull);

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
                .taxCode(managerResourceWk.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionPspRequest = new Institution();
        institutionPspRequest.setInstitutionType(InstitutionType.GSP);
        request.setInstitution(institutionPspRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResourceWk)));

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(),any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(orchestrationApi.apiStartOnboardingOrchestrationGet(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse())));

        asserter.assertThat(() -> onboardingService.onboarding(request, users), response -> {
            Assertions.assertEquals(request.getProductId(), response.getProductId());
            Assertions.assertNull(response.getUsers().get(0).getUserMailUuid());
        });

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    @Test
    @RunOnVertxContext
    void onboarding_whenUserFoundedAndWillUpdateMailUuid(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResourceWk.getFiscalCode())
                .role(PartyRole.MANAGER)
                .email("example@live.it")
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionPspRequest = new Institution();
        institutionPspRequest.setInstitutionType(InstitutionType.GSP);
        request.setInstitution(institutionPspRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResourceWk)));

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(),any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(orchestrationApi.apiStartOnboardingOrchestrationGet(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse())));

        asserter.assertThat(() -> onboardingService.onboarding(request, users), response -> {
            Assertions.assertEquals(request.getProductId(), response.getProductId());
            Assertions.assertNotNull(response.getUsers().get(0).getUserMailUuid());
        });

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenUserNotFoundedAndWillSave(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId("productId");
        request.setInstitution(new Institution());
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
                    onboardings.get(0).setId(UUID.randomUUID().toString());
                    return Uni.createFrom().nullItem();
                }));

        asserter.assertThat(() -> onboardingService.onboarding(request, users), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    @Test
    @RunOnVertxContext
    void onboarding_shouldThrowExceptionIfUserRegistryFails(UniAsserter asserter) {
        Onboarding onboardingDefaultRequest = new Onboarding();
        onboardingDefaultRequest.setInstitution(new Institution());
        onboardingDefaultRequest.setProductId(PROD_INTEROP.getValue());
        List<UserRequest> users = List.of(manager);

        mockPersistOnboarding(asserter);

        mockSimpleProductValidAssert(onboardingDefaultRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().failure(new WebApplicationException())));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingDefaultRequest, users), WebApplicationException.class);
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

        asserter.assertFailedWith(() -> onboardingService.completeWithoutSignatureVerification(onboarding.getId(), null),
                InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void completeWithoutSignatureVerification(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter, onboarding.getId());

        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        final String filepath = "upload-file-path";
        when(azureBlobClient.uploadFile(any(),any(),any())).thenReturn(filepath);
        mockUpdateToken(asserter, filepath);

        asserter.assertThat(() -> onboardingService.completeWithoutSignatureVerification(onboarding.getId(), testFile),
                Assertions::assertNotNull);

    }

    /* can't be tested because on test the signature is disabled. we should find a workaround */
    //@Test
    @RunOnVertxContext
    void complete_shouldThrowExceptionWhenSignatureFail(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter, onboarding.getId());

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

        asserter.assertFailedWith(() -> onboardingService.complete(onboarding.getId(), testFile),
                InvalidRequestException.class);
    }


    @Test
    @RunOnVertxContext
    void complete(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter, onboarding.getId());

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

        final String filepath = "upload-file-path";
        when(azureBlobClient.uploadFile(any(),any(),any())).thenReturn(filepath);
        mockUpdateToken(asserter, filepath);

        asserter.assertThat(() -> onboardingService.complete(onboarding.getId(), testFile),
                Assertions::assertNotNull);
    }
    @Test
    void testOnboardingGet() {
        int page = 0, size = 3;
        Onboarding onboarding = createDummyOnboarding();
        mockFindOnboarding(onboarding);
        OnboardingGetResponse getResponse = getOnboardingGetResponse(onboarding);
        UniAssertSubscriber<OnboardingGetResponse> subscriber = onboardingService
                .onboardingGet("prod-io", null, null, "2023-11-10", "2021-12-10", page,size)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(getResponse);
    }

    private OnboardingGetResponse getOnboardingGetResponse(Onboarding onboarding) {
        OnboardingGet onboardingGet = onboardingMapper.toGetResponse(onboarding);
        OnboardingGetResponse response = new OnboardingGetResponse();
        response.setCount(1L);
        response.setItems(List.of(onboardingGet));
        return response;
    }

    private void mockFindOnboarding(Onboarding onboarding) {
        ReactivePanacheQuery query = mock(ReactivePanacheQuery.class);
        ReactivePanacheQuery<Onboarding> queryPage = mock(ReactivePanacheQuery.class);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.find(any(Document.class),any(Document.class))).thenReturn(query);
        when(Onboarding.find(any(Document.class),eq(null))).thenReturn(query);
        when(query.page(anyInt(),anyInt())).thenReturn(queryPage);
        when(queryPage.list()).thenReturn(Uni.createFrom().item(List.of(onboarding)));
        when(query.count()).thenReturn(Uni.createFrom().item(1L));
    }

    private void mockFindToken(UniAsserter asserter, String onboardingId) {
        Token token = new Token();
        token.setChecksum("actual-checksum");
        asserter.execute(() -> PanacheMock.mock(Token.class));
        asserter.execute(() -> when(Token.list("onboardingId", onboardingId))
                .thenReturn(Uni.createFrom().item(List.of(token))));
    }

    private void mockUpdateToken(UniAsserter asserter, String filepath) {

        //Mock token updat
        asserter.execute(() -> PanacheMock.mock(Token.class));
        ReactivePanacheUpdate panacheUpdate = mock(ReactivePanacheUpdate.class);
        asserter.execute(() -> when(panacheUpdate.where("contractSigned", filepath))
                .thenReturn(Uni.createFrom().item(1L)));
        asserter.execute(() -> when(Token.update(anyString(),any(Object[].class)))
                .thenReturn(panacheUpdate));
    }

    private Onboarding createDummyOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(UUID.randomUUID().toString());
        onboarding.setProductId("prod-id");

        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
        institution.setSubunitCode("subunitCode");
        onboarding.setInstitution(institution);

        User user = new User();
        user.setId("actual-user-id");
        user.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(user));
        return onboarding;
    }

    @Test
    void testOnboardingUpdateStatusOK() {

        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));

        mockUpdateOnboarding(onboarding.getId(), 1L);
        UniAssertSubscriber<Long> subscriber = onboardingService
                .rejectOnboarding(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(1L);
    }

    @Test
    void rejectOnboarding_statusIsCOMPLETED() {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));

        mockUpdateOnboarding(onboarding.getId(), 1L);
        UniAssertSubscriber<Long> subscriber = onboardingService
                .rejectOnboarding(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void testOnboardingDeleteOnboardingNotFoundOrAlreadyDeleted() {

        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));
        mockUpdateOnboarding(onboarding.getId(), 0L);

        UniAssertSubscriber<Long> subscriber = onboardingService
                .rejectOnboarding(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    private void mockUpdateOnboarding(String onboardingId, Long updatedItemCount) {
        ReactivePanacheUpdate query = mock(ReactivePanacheUpdate.class);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.update(Onboarding.Fields.status.name(), OnboardingStatus.REJECTED)).thenReturn(query);
        when(query.where("_id", onboardingId)).thenReturn(Uni.createFrom().item(updatedItemCount));
    }

    @Test
    void onboardingGet() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        UniAssertSubscriber<OnboardingGet> subscriber = onboardingService
                .onboardingGet(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        OnboardingGet actual = subscriber.assertCompleted().awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(onboarding.getId(), actual.getId());
        Assertions.assertEquals(onboarding.getProductId(), actual.getProductId());
    }

    @Test
    void onboardingGet_shouldResourceNotFound() {
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        UniAssertSubscriber<OnboardingGet> subscriber = onboardingService
                .onboardingGet(UUID.randomUUID().toString())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ResourceNotFoundException.class);
    }

    @Test
    void onboardingPending() {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.PENDING);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        UniAssertSubscriber<OnboardingGet> subscriber = onboardingService
                .onboardingPending(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        OnboardingGet actual = subscriber.assertCompleted().awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(onboarding.getId(), actual.getId());
        Assertions.assertEquals(onboarding.getProductId(), actual.getProductId());
    }

    @Test
    void onboardingPending_shouldResourceNotFound() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        onboardingService
                .onboardingPending(UUID.randomUUID().toString())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ResourceNotFoundException.class);
    }

    @Test
    void onboardingGetWithUserInfo() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        when(userRegistryApi.findByIdUsingGET(anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(managerResource));

        UniAssertSubscriber<OnboardingGet> subscriber = onboardingService
                .onboardingGetWithUserInfo(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        OnboardingGet actual = subscriber.assertCompleted().awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(onboarding.getId(), actual.getId());
        Assertions.assertEquals(onboarding.getProductId(), actual.getProductId());
        Assertions.assertEquals(onboarding.getUsers().size(), actual.getUsers().size());
        UserResponse actualUser = actual.getUsers().get(0);
        Assertions.assertEquals(actualUser.getName(), managerResource.getName().getValue());
        Assertions.assertEquals(actualUser.getSurname(), managerResource.getFamilyName().getValue());
    }



    @Test
    void approve() {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(createDummyProduct(onboarding.getProductId(), false));

        when(onboardingApi.verifyOnboardingInfoUsingHEAD(onboarding.getInstitution().getTaxCode(), onboarding.getProductId(),
                onboarding.getInstitution().getSubunitCode()))
                .thenReturn(Uni.createFrom().failure(new ClientWebApplicationException(404)));

        when(orchestrationApi.apiStartOnboardingOrchestrationGet(onboarding.getId(), null))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse()));

        UniAssertSubscriber<OnboardingGet> subscriber = onboardingService
                .approve(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        OnboardingGet actual = subscriber.awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(onboarding.getId(), actual.getId());
    }

    @Test
    void approve_throwExceptionIfOnboardingIsNotToBeValidated() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(createDummyProduct(onboarding.getProductId(), false));

        onboardingService
                .approve(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void approveCompletion() {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(createDummyProduct(onboarding.getProductId(), false));

        when(onboardingApi.verifyOnboardingInfoUsingHEAD(onboarding.getInstitution().getTaxCode(), onboarding.getProductId(),
                onboarding.getInstitution().getSubunitCode()))
                .thenReturn(Uni.createFrom().failure(new ClientWebApplicationException(404)));

        UniAssertSubscriber<OnboardingGet> subscriber = onboardingService
                .approve(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        OnboardingGet actual = subscriber.awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(onboarding.getId(), actual.getId());

        verify(orchestrationApi, times(1))
                .apiStartOnboardingOrchestrationGet(onboarding.getId(), null);
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_importPA(UniAsserter asserter) {
        UserRequest manager = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setTaxCode("taxCode");
        request.setInstitution(institutionBaseRequest);
        OnboardingImportContract contractImported = new OnboardingImportContract();
        contractImported.setFileName("filename");
        contractImported.setFilePath("filepath");
        contractImported.setCreatedAt(LocalDateTime.now());
        contractImported.setContractType("type");

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(),any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        asserter.assertThat(() -> onboardingService.onboardingImport(request, users, contractImported), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }



    void mockPersistOnboarding(UniAsserter asserter) {
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.persist(any(Onboarding.class), any()))
                .thenAnswer(arg -> {
                    Onboarding onboarding = (Onboarding) arg.getArguments()[0];
                    onboarding.setId(UUID.randomUUID().toString());
                    return Uni.createFrom().nullItem();
                }));
    }

    void mockPersistToken(UniAsserter asserter) {
        asserter.execute(() -> PanacheMock.mock(Token.class));
        asserter.execute(() -> when(Token.persist(any(Token.class), any()))
                .thenAnswer(arg -> {
                    Token token = (Token) arg.getArguments()[0];
                    token.setId(UUID.randomUUID().toString());
                    return Uni.createFrom().nullItem();
                }));
    }
}
