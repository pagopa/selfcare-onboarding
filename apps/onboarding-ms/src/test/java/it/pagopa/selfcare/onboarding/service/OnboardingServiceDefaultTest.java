package it.pagopa.selfcare.onboarding.service;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
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
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserId;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(InvalidRequestException.class);
    }


    @Test
    void onboardingPa_throwExceptionIfUserFoundedAndProductIsNotValid() {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new InstitutionBaseRequest());

        when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource));

        when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(null);

        onboardingService.onboardingPa(onboardingRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(OnboardingNotAllowedException.class);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(productService, times(1))
                .getProductIsValid(onboardingRequest.getProductId());
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

        when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource));

        Product productResource = new Product();
        productResource.setDelegable(Boolean.FALSE);
        when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource);

        onboardingService.onboardingPa(onboardingRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(OnboardingNotAllowedException.class);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(productService, times(1))
                .getProductIsValid(onboardingRequest.getProductId());
        verifyNoMoreInteractions(userRegistryApi);
    }

    @Test
    void onboardingPa_throwExceptionIfUserFoundedAndProductRoleIsNotValid() {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new InstitutionBaseRequest());

        when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource));

        Product productResource = new Product();
        productResource.setRoleMappings(new HashMap<>());
        when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource);

        onboardingService.onboardingPa(onboardingRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(OnboardingNotAllowedException.class);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(productService, times(1))
                .getProductIsValid(onboardingRequest.getProductId());
        verifyNoMoreInteractions(userRegistryApi);
    }

    @Test
    void onboardingPa_throwExceptionIfUserFoundedAndProductParentRoleIsNotValid() {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        onboardingRequest.setInstitution(new InstitutionBaseRequest());

        when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource));

        Product productResource = new Product();
        Product productParent = new Product();
        productParent.setRoleMappings(new HashMap<>());
        productResource.setParent(productParent);
        when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource);

        onboardingService.onboardingPa(onboardingRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(OnboardingNotAllowedException.class);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(productService, times(1))
                .getProductIsValid(onboardingRequest.getProductId());
        verifyNoMoreInteractions(userRegistryApi);
    }

    @Test
    void onboardingPa_throwExceptionIfProductAlreadyOnboarded() {
        OnboardingPaRequest onboardingRequest = new OnboardingPaRequest();
        onboardingRequest.setUsers(List.of(manager));
        onboardingRequest.setProductId("productId");
        InstitutionBaseRequest institutionBaseRequest = new InstitutionBaseRequest();
        institutionBaseRequest.setTaxCode("taxCode");
        onboardingRequest.setInstitution(institutionBaseRequest);

        when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().item(managerResource));

        mockSimpleProductValid(onboardingRequest.getProductId());

        when(onboardingApi.verifyOnboardingInfoUsingHEAD(institutionBaseRequest.getTaxCode(), onboardingRequest.getProductId(), institutionBaseRequest.getSubunitCode()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build()));

        onboardingService.onboardingPa(onboardingRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(InvalidRequestException.class);

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verify(onboardingApi, times(1))
                .verifyOnboardingInfoUsingHEAD(institutionBaseRequest.getTaxCode(), onboardingRequest.getProductId(), institutionBaseRequest.getSubunitCode());
        verifyNoMoreInteractions(userRegistryApi);
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
        onboardingRequest.setInstitution(new InstitutionBaseRequest());

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.onboardingSa(onboardingRequest), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    void mockSimpleProductValid(String productId) {
        Product productResource = createDummyProduct(productId,false);

        when(productService.getProductIsValid(productId))
                .thenReturn(productResource);
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
        onboardingRequest.setInstitution(new InstitutionPspRequest());

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.onboardingPsp(onboardingRequest), Assertions::assertNotNull);

        asserter.execute(() -> {
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
        onboardingRequest.setInstitution(new InstitutionPspRequest());

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), true, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.onboardingPsp(onboardingRequest), Assertions::assertNotNull);

        asserter.execute(() -> {
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

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingDefaultRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.onboarding(onboardingDefaultRequest), Assertions::assertNotNull);

        asserter.execute(() -> {
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
        request.setProductId("productId");
        request.setInstitution(new InstitutionBaseRequest());

        when(userRegistryApi.updateUsingPATCH(any(),any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build()));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound(asserter);

        asserter.assertThat(() -> onboardingService.onboarding(request), Assertions::assertNotNull);

        asserter.execute(() -> {
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
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    @Test
    void onboarding_shouldThrowExceptionIfUserRegistryFails() {
        OnboardingDefaultRequest onboardingDefaultRequest = new OnboardingDefaultRequest();
        onboardingDefaultRequest.setUsers(List.of(manager));

        when(userRegistryApi.searchUsingPOST(any(),any()))
                .thenReturn(Uni.createFrom().failure(new WebApplicationException()));

        onboardingService.onboarding(onboardingDefaultRequest)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).awaitFailure();

        verify(userRegistryApi, times(1))
                .searchUsingPOST(any(),any());
        verifyNoMoreInteractions(userRegistryApi);
    }

    void mockVerifyOnboardingNotFound(UniAsserter asserter){
        asserter.execute(() -> when(onboardingApi.verifyOnboardingInfoUsingHEAD(any(), any(), any()))
                .thenReturn(Uni.createFrom().failure(new ClientWebApplicationException(404))));
    }
}
