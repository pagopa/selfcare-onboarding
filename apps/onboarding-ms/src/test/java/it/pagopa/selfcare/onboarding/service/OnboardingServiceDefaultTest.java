package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.controller.request.AggregateInstitutionRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingImportContract;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingUserRequest;
import it.pagopa.selfcare.onboarding.controller.request.UserRequest;
import it.pagopa.selfcare.onboarding.controller.response.*;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceConflictException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapperImpl;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import it.pagopa.selfcare.onboarding.service.profile.OnboardingTestProfile;
import it.pagopa.selfcare.onboarding.service.strategy.OnboardingValidationStrategy;
import it.pagopa.selfcare.onboarding.service.util.OnboardingUtils;
import it.pagopa.selfcare.product.entity.PHASE_ADDITION_ALLOWED;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.api.OnboardingApi;
import org.openapi.quarkus.core_json.model.InstitutionsResponse;
import org.openapi.quarkus.onboarding_functions_json.api.OrchestrationApi;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.*;
import org.openapi.quarkus.party_registry_proxy_json.model.*;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserId;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

import static it.pagopa.selfcare.onboarding.common.InstitutionType.PSP;
import static it.pagopa.selfcare.onboarding.common.ProductId.*;
import static it.pagopa.selfcare.onboarding.common.WorkflowType.INCREMENT_REGISTRATION_AGGREGATOR;
import static it.pagopa.selfcare.onboarding.service.OnboardingServiceDefault.USERS_FIELD_LIST;
import static it.pagopa.selfcare.onboarding.service.OnboardingServiceDefault.USERS_FIELD_TAXCODE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.openapi.quarkus.core_json.model.InstitutionProduct.StateEnum.PENDING;


@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class OnboardingServiceDefaultTest {

    @Inject
    OnboardingServiceDefault onboardingService;

    @InjectMock
    @RestClient
    UserApi userRegistryApi;

    @InjectMock
    @RestClient
    InsuranceCompaniesApi insuranceCompaniesApi;

    @InjectMock
    ProductService productService;

    @InjectMock
    @RestClient
    AooApi aooApi;

    @InjectMock
    @RestClient
    InstitutionApi institutionApi;

    @InjectMock
    @RestClient
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionRegistryProxyApi;

    @InjectMock
    @RestClient
    UoApi uoApi;

    @InjectMock
    @RestClient
    InfocamerePdndApi infocamerePdndApi;

    @RestClient
    @InjectMock
    InfocamereApi infocamereApi;

    @RestClient
    @InjectMock
    NationalRegistriesApi nationalRegistriesApi;

    @RestClient
    @InjectMock
    org.openapi.quarkus.user_json.api.InstitutionApi userInstitutionApi;

    @InjectMock
    AzureBlobClient azureBlobClient;

    @InjectMock
    SignatureService signatureService;

    @InjectMock
    TokenService tokenService;

    @InjectMock
    @RestClient
    OnboardingApi onboardingApi;

    @InjectMock
    @RestClient
    OrchestrationApi orchestrationApi;

    @InjectMock
    @RestClient
    GeographicTaxonomiesApi geographicTaxonomiesApi;

    @InjectMock
    OnboardingValidationStrategy onboardingValidationStrategy;

    @Spy
    OnboardingMapper onboardingMapper = new OnboardingMapperImpl();

    static final UserRequest manager = UserRequest.builder()
            .name("name")
            .surname("surname")
            .taxCode("taxCode")
            .role(PartyRole.MANAGER)
            .build();

    static final UserResource managerResource;
    static final UserResource managerResourceWk;
    static final UserResource managerResourceWkSpid;

    static final String productRoleAdminCode = "admin";
    static final String productRoleAdminPspCode = "admin-psp";
    static final String DIGITAL_ADDRESS_FIELD = "digitalAddress";
    static final String DESCRIPTION_FIELD = "description";
    static final File testFile = new File("src/test/resources/application.properties");

    static final FormItem TEST_FORM_ITEM = FormItem.builder()
            .fileName("testFile")
            .file(testFile)
            .build();

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

        managerResourceWkSpid = new UserResource();
        managerResourceWkSpid.setId(UUID.randomUUID());
        managerResourceWkSpid.setName(new CertifiableFieldResourceOfstring()
                .value(manager.getName())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.SPID));
        managerResourceWkSpid.setFamilyName(new CertifiableFieldResourceOfstring()
                .value(manager.getSurname())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.SPID));
        managerResourceWkSpid.setWorkContacts(map);
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

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null), OnboardingNotAllowedException.class);
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

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null), OnboardingNotAllowedException.class);
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
        mockVerifyAllowedMap(onboardingRequest.getInstitution().getTaxCode(), onboardingRequest.getProductId(), asserter);
        mockVerifyOnboardingNotEmpty(asserter);

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null), ResourceConflictException.class);
    }

    @Test
    @RunOnVertxContext
    void onboardingIncrement_Ok(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        onboardingRequest.setIsAggregator(true);
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setOriginId("originId");
        onboardingRequest.setInstitution(institutionBaseRequest);
        Billing billing = new Billing();
        billing.setRecipientCode("recCode");
        onboardingRequest.setBilling(billing);

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setDescription("test");
        onboardingRequest.setAggregates(List.of(aggregateInstitution));

        AggregateInstitutionRequest aggregateInstitutionRequest = new AggregateInstitutionRequest();
        aggregateInstitutionRequest.setDescription("test");
        aggregateInstitutionRequest.setTaxCode("taxCode");

        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter);
        mockVerifyAllowedMap(onboardingRequest.getInstitution().getTaxCode(), onboardingRequest.getProductId(), asserter);
        mockVerifyOnboardingNotEmpty(asserter);

        UOResource uoResource = new UOResource();
        uoResource.setCodiceFiscaleSfe("codSfe");
        uoResource.setCodiceIpa("originId");
        when(uoApi.findByUnicodeUsingGET1("recCode", null)).thenReturn(Uni.createFrom().item(uoResource));

        managerResource.setId(UUID.fromString("9456d91f-ef53-4f89-8330-7f9a195d5d1e"));
        when(userRegistryApi.searchUsingPOST(eq(USERS_FIELD_LIST), any())).thenReturn(Uni.createFrom().item(managerResource));

        OnboardingResponse onboardingResponse = getOnboardingResponse();

        Uni<OnboardingResponse> response = onboardingService.onboardingIncrement(onboardingRequest, users, List.of(aggregateInstitutionRequest));

        asserter.assertEquals(() -> response, onboardingResponse);
    }

    private static OnboardingResponse getOnboardingResponse() {
        OnboardingResponse onboardingResponse = new OnboardingResponse();
        onboardingResponse.setWorkflowType(INCREMENT_REGISTRATION_AGGREGATOR.toString());
        onboardingResponse.setProductId("productId");
        it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse institution = new it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse();
        institution.setInstitutionType("PA");
        institution.setOriginId("originId");
        institution.setTaxCode("taxCode");
        onboardingResponse.setInstitution(institution);

        UserOnboardingResponse userOnboardingResponse = new UserOnboardingResponse();
        userOnboardingResponse.setId("9456d91f-ef53-4f89-8330-7f9a195d5d1e");
        userOnboardingResponse.setRole(PartyRole.MANAGER);
        userOnboardingResponse.setProductRole("admin");
        onboardingResponse.setUsers(List.of(userOnboardingResponse));

        BillingResponse billingResponse = new BillingResponse();
        billingResponse.setRecipientCode("recCode");
        onboardingResponse.setBilling(billingResponse);

        onboardingResponse.setStatus(PENDING.toString());
        onboardingResponse.setIsAggregator(true);
        return onboardingResponse;
    }


    @Test
    @RunOnVertxContext
    void onboardingIncrement_throwExceptionIfProductNotOnboarded(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        onboardingRequest.setInstitution(institutionBaseRequest);

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setDescription("test");
        onboardingRequest.setAggregates(List.of(aggregateInstitution));

        AggregateInstitutionRequest aggregateInstitutionRequest = new AggregateInstitutionRequest();
        aggregateInstitutionRequest.setDescription("test");
        aggregateInstitutionRequest.setTaxCode("taxCode");

        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter);
        mockVerifyAllowedMap(onboardingRequest.getInstitution().getTaxCode(), onboardingRequest.getProductId(), asserter);
        mockVerifyOnboardingNotFound();

        UOResource uoResource = new UOResource();
        uoResource.setCodiceFiscaleSfe("codSfe");
        uoResource.setCodiceIpa("originId");
        when(uoApi.findByUnicodeUsingGET1(any(), any())).thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setDescription(DESCRIPTION_FIELD);
        when(institutionRegistryProxyApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(institutionResource));

        asserter.assertFailedWith(() -> onboardingService.onboardingIncrement(onboardingRequest, users, List.of(aggregateInstitutionRequest)), InvalidRequestException.class);
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

        mockVerifyOnboardingNotFound();

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null), OnboardingNotAllowedException.class);
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

        mockVerifyOnboardingNotFound();
        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null), OnboardingNotAllowedException.class);
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

        mockVerifyOnboardingNotFound();
        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null), OnboardingNotAllowedException.class);
    }


    @Test
    @RunOnVertxContext
    void onboarding_shouldThrowExceptionIfRoleNotValid(UniAsserter asserter) {
        Onboarding onboardingDefaultRequest = new Onboarding();
        Institution institution = dummyInstitution();
        institution.setOrigin(Origin.IVASS);
        institution.setDescription(DESCRIPTION_FIELD);
        institution.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        onboardingDefaultRequest.setInstitution(institution);
        onboardingDefaultRequest.setProductId(PROD_INTEROP.getValue());

        List<UserRequest> users = List.of(UserRequest.builder()
                .taxCode("taxCode")
                .role(PartyRole.OPERATOR)
                .build());

        mockSimpleProductValidAssert(onboardingDefaultRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockPersistOnboarding(asserter);
        mockVerifyAllowedMap(onboardingDefaultRequest.getInstitution().getTaxCode(), onboardingDefaultRequest.getProductId(), asserter);

        InsuranceCompanyResource insuranceCompanyResource = new InsuranceCompanyResource();
        insuranceCompanyResource.setDescription(DESCRIPTION_FIELD);
        insuranceCompanyResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        when(insuranceCompaniesApi.searchByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(insuranceCompanyResource));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingDefaultRequest, users, null),
                InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_Aoo(UniAsserter asserter) {
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
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);


        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();

        AOOResource aooResource = new AOOResource();
        aooResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(aooApi.findByUnicodeUsingGET(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().item(aooResource)));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_AooNotFound(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.AOO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        AOOResource aooResource = new AOOResource();
        aooResource.setDenominazioneEnte("TEST");
        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(exception.getResponse()).thenReturn(response);
        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().item(uoResource)));
        asserter.execute(() -> when(aooApi.findByUnicodeUsingGET(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().failure(exception)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null), ResourceNotFoundException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_AooException(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.AOO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        AOOResource aooResource = new AOOResource();
        aooResource.setDenominazioneEnte("TEST");
        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(exception.getResponse()).thenReturn(response);

        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().item(uoResource)));

        asserter.execute(() -> when(aooApi.findByUnicodeUsingGET(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().failure(exception)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null), WebApplicationException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_Uo(UniAsserter asserter) {
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

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().item(uoResource)));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_UoNotFound(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.UO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException("Resource not found");

        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        uoResource.setCodiceFiscaleEnte("taxCode");
        when(institutionRegistryProxyApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(new InstitutionResource()));
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().failure(resourceNotFoundException)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null), ResourceNotFoundException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_UoException(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.UO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(exception.getResponse()).thenReturn(response);

        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().failure(exception)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null), WebApplicationException.class);
    }

    void mockSimpleSearchPOSTAndPersist(UniAsserter asserter) {

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(), any()))
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
    void onboarding_PRV_PagoPA(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_PAGOPA.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setDigitalAddress("pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode");
        request.setInstitution(institutionBaseRequest);
        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessName("name");
        pdndBusinessResource.setDigitalAddress("pec");

        when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(pdndBusinessResource));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_PRV(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setDigitalAddress("pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode");
        request.setInstitution(institutionBaseRequest);
        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessName("name");
        pdndBusinessResource.setDigitalAddress("pec");

        when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(pdndBusinessResource));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_PRV_Bad_Request(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setDescription("nome");
        institutionBaseRequest.setDigitalAddress("pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode");
        request.setInstitution(institutionBaseRequest);
        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessName("name");
        pdndBusinessResource.setDigitalAddress("pec");

        asserter.execute(() -> when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any()))
                .thenReturn(Uni.createFrom().item(pdndBusinessResource)));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null), InvalidRequestException.class);

    }

    @Test
    @RunOnVertxContext
    void onboarding_PRV_Bad_Request_Pec(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setDigitalAddress("wrong-pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode");
        request.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessName("name");
        pdndBusinessResource.setDigitalAddress("pec");

        when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(pdndBusinessResource));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null), InvalidRequestException.class);

    }

    @Test
    @RunOnVertxContext
    void onboarding_PRV_Institution_Not_Found(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setDigitalAddress("wrong-pec");
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode");
        request.setInstitution(institutionBaseRequest);
        List<AggregateInstitution> aggregates = new ArrayList<>();
        AggregateInstitution institution = new AggregateInstitution();
        aggregates.add(institution);
        request.setAggregates(aggregates);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        ClientWebApplicationException exception = new ClientWebApplicationException(HttpStatus.SC_NOT_FOUND);
        when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().failure(exception));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null), ResourceNotFoundException.class);

    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_Aggregator(UniAsserter asserter) {
        UserRequest managerUser = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        Onboarding request = new Onboarding();
        request.setIsAggregator(Boolean.TRUE);
        List<UserRequest> users = List.of(managerUser);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        request.setBilling(billing);
        request.setInstitution(institutionBaseRequest);
        List<AggregateInstitution> aggregates = new ArrayList<>();
        AggregateInstitution institution = new AggregateInstitution();
        aggregates.add(institution);
        request.setAggregates(aggregates);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setDescription(DESCRIPTION_FIELD);
        when(institutionRegistryProxyApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(institutionResource));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_Aggregator_WithUsers(UniAsserter asserter) {
        UserRequest managerUser = UserRequest.builder()
                .name("name")
                .taxCode(managerResource.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        Onboarding request = new Onboarding();
        request.setIsAggregator(Boolean.TRUE);
        request.setProductId(PROD_INTEROP.getValue());

        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        request.setInstitution(institutionBaseRequest);
        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        request.setBilling(billing);
        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setTaxCode("taxCode");

        AggregateInstitutionRequest aggregateInstitutionRequest = new AggregateInstitutionRequest();
        aggregateInstitutionRequest.setTaxCode("taxCode");
        aggregateInstitutionRequest.setUsers(List.of(managerUser));

        List<AggregateInstitution> aggregates = new ArrayList<>();
        aggregates.add(aggregateInstitution);
        request.setAggregates(aggregates);

        List<UserRequest> users = List.of(managerUser);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, List.of(aggregateInstitutionRequest)), Assertions::assertNotNull);

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setDescription(DESCRIPTION_FIELD);
        when(institutionRegistryProxyApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(institutionResource));

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboardingSa_whenUserFoundedAndWillNotUpdate(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IVASS);
        institutionBaseRequest.setInstitutionType(InstitutionType.SA);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        onboardingRequest.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(onboardingRequest.getInstitution().getTaxCode(), onboardingRequest.getProductId(), asserter);

        InsuranceCompanyResource insuranceCompanyResource = new InsuranceCompanyResource();
        insuranceCompanyResource.setDescription(DESCRIPTION_FIELD);
        insuranceCompanyResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        when(insuranceCompaniesApi.searchByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(insuranceCompanyResource));

        asserter.assertThat(() -> onboardingService.onboarding(onboardingRequest, users, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    Product mockSimpleProductValidAssert(String productId, boolean hasParent, UniAsserter asserter) {
        Product productResource = createDummyProduct(productId, hasParent);
        asserter.execute(() -> when(productService.getProductIsValid(productId))
                .thenReturn(productResource));
        return productResource;
    }

    ProductRoleInfo dummyProductRoleInfo(String productRolCode) {
        ProductRole productRole = new ProductRole();
        productRole.setCode(productRolCode);
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setRoles(List.of(productRole));
        productRoleInfo.setPhasesAdditionAllowed(List.of(PHASE_ADDITION_ALLOWED.ONBOARDING.value));
        return productRoleInfo;
    }

    Product createDummyProduct(String productId, boolean hasParent) {

        Map<PartyRole, ProductRoleInfo> roleMappingByInstitutionType = new HashMap<>();
        roleMappingByInstitutionType.put(manager.getRole(), dummyProductRoleInfo(productRoleAdminPspCode));

        Product productResource = new Product();
        productResource.setId(productId);
        productResource.setRoleMappings(Map.of(manager.getRole(), dummyProductRoleInfo(productRoleAdminCode)));
        productResource.setRoleMappingsByInstitutionType(Map.of(PSP.name(), roleMappingByInstitutionType));
        productResource.setTitle("title");

        if (hasParent) {
            Product parent = new Product();
            parent.setId("productParentId");
            parent.setRoleMappings(Map.of(manager.getRole(), dummyProductRoleInfo(productRoleAdminCode)));
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
        institutionPspRequest.setInstitutionType(PSP);
        onboardingRequest.setInstitution(institutionPspRequest);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(onboardingRequest.getInstitution().getTaxCode(), onboardingRequest.getProductId(), asserter);

        asserter.assertThat(() -> onboardingService.onboarding(onboardingRequest, users, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    @Test
    @RunOnVertxContext
    void onboardingPsp_whenUserFoundedAndWillNotUpdateAndProductHasParent(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productParentId");
        Institution institutionPspRequest = new Institution();
        institutionPspRequest.setOrigin(Origin.SELC);
        institutionPspRequest.setInstitutionType(InstitutionType.PSP);
        institutionPspRequest.setInstitutionType(PSP);
        institutionPspRequest.setTaxCode("taxCode");
        onboardingRequest.setInstitution(institutionPspRequest);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), true, asserter);
        mockVerifyAllowedMap(onboardingRequest.getInstitution().getTaxCode(), onboardingRequest.getProductId(), asserter);

        // mock parent has already onboarding
        mockVerifyOnboardingNotFound();
        asserter.assertThat(() -> onboardingService.onboarding(onboardingRequest, users, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    Institution dummyInstitution() {
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.SA);
        return institution;
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenUserFoundedAndWillNotUpdate(UniAsserter asserter) {
        Onboarding onboardingDefaultRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingDefaultRequest.setProductId("productId");
        Institution institution = dummyInstitution();
        institution.setOrigin(Origin.IVASS);
        institution.setDescription(DESCRIPTION_FIELD);
        institution.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        onboardingDefaultRequest.setInstitution(institution);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingDefaultRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(onboardingDefaultRequest.getInstitution().getTaxCode(), onboardingDefaultRequest.getProductId(), asserter);
        InsuranceCompanyResource insuranceCompanyResource = new InsuranceCompanyResource();
        insuranceCompanyResource.setDescription(DESCRIPTION_FIELD);
        insuranceCompanyResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        when(insuranceCompaniesApi.searchByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(insuranceCompanyResource));

        asserter.assertThat(() -> onboardingService.onboarding(onboardingDefaultRequest, users, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenUserFoundedAndWillUpdate(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        request.setBilling(billing);
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionPspRequest = new Institution();
        institutionPspRequest.setDescription(DESCRIPTION_FIELD);
        institutionPspRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionPspRequest.setOrigin(Origin.IPA);
        institutionPspRequest.setInstitutionType(InstitutionType.GSP);
        request.setInstitution(institutionPspRequest);
        AdditionalInformations adds = new AdditionalInformations();
        adds.setIpa(false);
        adds.setOtherNote("other");
        request.setAdditionalInformations(adds);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(managerResourceWk)));

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockPersistOnboarding(asserter);

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));
        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setDescription(DESCRIPTION_FIELD);
        when(institutionRegistryProxyApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(institutionResource));

        asserter.execute(() -> when(orchestrationApi.apiStartOnboardingOrchestrationGet(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse())));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null), response -> {
            Assertions.assertEquals(request.getProductId(), response.getProductId());
            Assertions.assertNull(response.getUsers().get(0).getUserMailUuid());
        });

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenUserFoundAndWillNotUpdate(UniAsserter asserter) {
        UserRequest wrongManager = UserRequest.builder()
                .name("wrong_name")
                .taxCode(managerResourceWkSpid.getFiscalCode())
                .role(PartyRole.MANAGER)
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(wrongManager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionPspRequest = new Institution();
        institutionPspRequest.setInstitutionType(InstitutionType.GSP);
        request.setInstitution(institutionPspRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(managerResourceWkSpid)));

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockPersistOnboarding(asserter);

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null), InvalidRequestException.class);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    @Test
    @RunOnVertxContext
    void onboarding_whenUserFoundedAndWillUpdateMailUuid(UniAsserter asserter) {
        UserRequest newManager = UserRequest.builder()
                .name("name")
                .taxCode(managerResourceWk.getFiscalCode())
                .role(PartyRole.MANAGER)
                .email("example@live.it")
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(newManager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionPspRequest = new Institution();
        institutionPspRequest.setInstitutionType(InstitutionType.GSP);
        request.setInstitution(institutionPspRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(managerResourceWk)));

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(orchestrationApi.apiStartOnboardingOrchestrationGet(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse())));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null), response -> {
            Assertions.assertEquals(request.getProductId(), response.getProductId());
            Assertions.assertNotNull(response.getUsers().get(0).getUserMailUuid());
        });

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenUserNotFoundedAndWillSave(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId("productId");
        request.setInstitution(dummyInstitution());
        final UUID createUserId = UUID.randomUUID();

        mockPersistOnboarding(asserter);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().failure(new WebApplicationException(404))));

        asserter.execute(() -> when(userRegistryApi.saveUsingPATCH(any()))
                .thenReturn(Uni.createFrom().item(UserId.builder().id(createUserId).build())));

        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        asserter.execute(() -> when(Onboarding.persistOrUpdate(any(List.class)))
                .thenAnswer(arg -> {
                    List<Onboarding> onboardings = (List<Onboarding>) arg.getArguments()[0];
                    onboardings.get(0).setId(UUID.randomUUID().toString());
                    return Uni.createFrom().nullItem();
                }));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    @Test
    @RunOnVertxContext
    void onboarding_shouldThrowExceptionIfUserRegistryFails(UniAsserter asserter) {
        Onboarding onboardingDefaultRequest = new Onboarding();
        onboardingDefaultRequest.setInstitution(dummyInstitution());
        onboardingDefaultRequest.setProductId(PROD_INTEROP.getValue());
        List<UserRequest> users = List.of(manager);

        mockPersistOnboarding(asserter);

        mockSimpleProductValidAssert(onboardingDefaultRequest.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(onboardingDefaultRequest.getInstitution().getTaxCode(), onboardingDefaultRequest.getProductId(), asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().failure(new WebApplicationException())));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingDefaultRequest, users, null), WebApplicationException.class);
    }

    void mockVerifyOnboardingNotFound() {
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().empty());
        when(Onboarding.find(any())).thenReturn(query);
    }

    void mockVerifyOnboardingNotEmpty(UniAsserter asserter) {

        asserter.execute(() -> {
            PanacheMock.mock(Onboarding.class);
            ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
            when(query.stream()).thenReturn(Multi.createFrom().items(createDummyOnboarding()));
            when(Onboarding.find(any())).thenReturn(query);
        });

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
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(onboarding.getInstitution().getTaxCode(), onboarding.getProductId(), asserter);

        final String filepath = "upload-file-path";
        when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);
        mockUpdateToken(asserter, filepath);

        asserter.assertThat(() -> onboardingService.completeWithoutSignatureVerification(onboarding.getId(), TEST_FORM_ITEM),
                Assertions::assertNotNull);

    }

    @Test
    @RunOnVertxContext
    void completeWithoutSignatureValidation(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter, onboarding.getId());

        //Mock find managerUserfiscal code
        String actualUseUid = onboarding.getUsers().get(0).getId();
        UserResource actualUserResource = new UserResource();
        actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
        asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                .thenReturn(Uni.createFrom().item(actualUserResource)));

        //Mock contract signature fail
        asserter.execute(() -> doNothing()
                .when(signatureService)
                .verifySignature(any(), any(), any()));

        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(onboarding.getInstitution().getTaxCode(), onboarding.getProductId(), asserter);

        final String filepath = "upload-file-path";
        when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);
        mockUpdateToken(asserter, filepath);

        asserter.assertThat(() -> onboardingService.complete(onboarding.getId(), TEST_FORM_ITEM),
                Assertions::assertNotNull);
    }

    @Test
    @RunOnVertxContext
    void completeOnboardingUsersWithoutSignatureValidation(UniAsserter asserter) {
        Onboarding onboarding = createDummyUsersOnboarding();
        onboarding.setProductId("productParentId");
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter, onboarding.getId());

        //Mock find managerUserfiscal code
        String actualUseUid = onboarding.getUsers().get(0).getId();
        UserResource actualUserResource = new UserResource();
        actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
        asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                .thenReturn(Uni.createFrom().item(actualUserResource)));

        //Mock contract signature fail
        asserter.execute(() -> doNothing()
                .when(signatureService)
                .verifySignature(any(), any(), any()));

        mockVerifyAllowedMap(onboarding.getInstitution().getTaxCode(), onboarding.getProductId(), asserter);
        mockSimpleProductValidAssert(onboarding.getProductId(), true, asserter);
        mockVerifyOnboardingNotFound();

        final String filepath = "upload-file-path";
        when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);
        mockUpdateToken(asserter, filepath);

        asserter.assertThat(() -> onboardingService.completeOnboardingUsers(onboarding.getId(), TEST_FORM_ITEM),
                Assertions::assertNotNull);
    }

    @Test
    @RunOnVertxContext
    void completeOnboardingUsers_throwProductNotOnboardedInReferenceOnboarding(UniAsserter asserter) {
        Onboarding onboarding = createDummyUsersOnboarding();
        onboarding.setStatus(OnboardingStatus.PENDING);
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter, onboarding.getId());
        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter);
        mockVerifyAllowedMap(onboarding.getInstitution().getTaxCode(), onboarding.getProductId(), asserter);

        asserter.assertFailedWith(() -> onboardingService.completeOnboardingUsers(onboarding.getId(), TEST_FORM_ITEM),
                InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void completeOnboardingUsers_throwOnboardingNotAllowedException(UniAsserter asserter) {
        Onboarding onboarding = createDummyUsersOnboarding();
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter, onboarding.getId());
        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter);

        asserter.execute(() -> when(onboardingValidationStrategy.validate(onboarding.getProductId(), onboarding.getInstitution().getTaxCode()))
                .thenReturn(false));

        asserter.assertFailedWith(() -> onboardingService.completeOnboardingUsers(onboarding.getId(), TEST_FORM_ITEM),
                OnboardingNotAllowedException.class);
    }

    @Test
    @RunOnVertxContext
    void completeOnboardingUsers_throwInvalidRequestException(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter, onboarding.getId());
        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter);
        mockVerifyAllowedMap(onboarding.getInstitution().getTaxCode(), onboarding.getProductId(), asserter);

        asserter.assertFailedWith(() -> onboardingService.completeOnboardingUsers(onboarding.getId(), TEST_FORM_ITEM),
                InvalidRequestException.class);
    }

    @Test
    void testOnboardingGet() {
        int page = 0, size = 3;
        Onboarding onboarding = createDummyOnboarding();
        mockFindOnboarding(onboarding);
        OnboardingGetResponse getResponse = getOnboardingGetResponse(onboarding);
        OnboardingGetFilters filters = OnboardingGetFilters.builder()
                .productId("prod-io")
                .taxCode("taxCode")
                .from("2023-12-01")
                .to("2023-12-31")
                .status("ACTIVE")
                .page(page)
                .size(size)
                .build();
        UniAssertSubscriber<OnboardingGetResponse> subscriber = onboardingService
                .onboardingGet(filters)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(getResponse);
    }

    @Test
    void testOnboardingGet2() {
        int page = 0, size = 3;
        Onboarding onboarding = createDummyOnboarding();
        mockFindOnboarding(onboarding);
        OnboardingGetResponse getResponse = getOnboardingGetResponse(onboarding);
        OnboardingGetFilters filters = OnboardingGetFilters.builder()
                .productId("prod-io")
                .taxCode("taxCode")
                .subunitCode("subunitCode")
                .from("2023-12-01")
                .to("2023-12-31")
                .status("ACTIVE")
                .page(page)
                .size(size)
                .build();
        UniAssertSubscriber<OnboardingGetResponse> subscriber = onboardingService
                .onboardingGet(filters)
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
        when(Onboarding.find(any(Document.class), any(Document.class))).thenReturn(query);
        when(Onboarding.find(any(Document.class), eq(null))).thenReturn(query);
        when(query.page(anyInt(), anyInt())).thenReturn(queryPage);
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

        //Mock token update
        asserter.execute(() -> PanacheMock.mock(Token.class));
        ReactivePanacheUpdate panacheUpdate = mock(ReactivePanacheUpdate.class);
        asserter.execute(() -> when(panacheUpdate.where("contractSigned", filepath))
                .thenReturn(Uni.createFrom().item(1L)));
        asserter.execute(() -> when(Token.update(anyString(), any(Object[].class)))
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

    private Onboarding createDummyUsersOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(UUID.randomUUID().toString());
        onboarding.setProductId("prod-id");
        onboarding.setReferenceOnboardingId("referenceOnboardinId");
        onboarding.setStatus(OnboardingStatus.COMPLETED);

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
                .rejectOnboarding(onboarding.getId(), "string")
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
                .rejectOnboarding(onboarding.getId(), "string")
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
                .rejectOnboarding(onboarding.getId(), "string")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    private void mockUpdateOnboarding(String onboardingId, Long updatedItemCount) {
        ReactivePanacheUpdate query = mock(ReactivePanacheUpdate.class);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.update(any(Document.class))).thenReturn(query);
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
        onboardingService
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

        when(tokenService.getAttachments(any()))
                .thenReturn(Uni.createFrom().item(List.of("filename")));

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

        mockVerifyOnboardingNotFound();

        when(orchestrationApi.apiStartOnboardingOrchestrationGet(onboarding.getId(), null))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse()));

        when(onboardingValidationStrategy.validate(onboarding.getProductId(), onboarding.getInstitution().getTaxCode()))
                .thenReturn(true);

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

        mockVerifyOnboardingNotFound();

        when(onboardingValidationStrategy.validate(onboarding.getProductId(), onboarding.getInstitution().getTaxCode()))
                .thenReturn(true);

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
        Onboarding request = new Onboarding();
        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        request.setBilling(billing);
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setImported(true);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
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
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        institutionResource.setDescription(DESCRIPTION_FIELD);
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setIstatCode("istatCode");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCountryAbbreviation("IT");
        geographicTaxonomyResource.setProvinceAbbreviation("RM");
        geographicTaxonomyResource.setDesc("desc");
        asserter.execute(() -> when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
                .thenReturn(Uni.createFrom().item(geographicTaxonomyResource)));

        asserter.assertThat(() -> onboardingService.onboardingImport(request, users, contractImported, false), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_importPSP(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        request.setProductId(PROD_PAGOPA.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(PSP);
        PaymentServiceProvider paymentServiceProvider = new PaymentServiceProvider();
        paymentServiceProvider.setAbiCode("abiCode");
        institutionBaseRequest.setPaymentServiceProvider(paymentServiceProvider);
        institutionBaseRequest.setTaxCode("taxCode");
        request.setInstitution(institutionBaseRequest);
        OnboardingImportContract contractImported = new OnboardingImportContract();
        contractImported.setFileName("filename");
        contractImported.setFilePath("filepath");
        contractImported.setCreatedAt(LocalDateTime.now());
        contractImported.setActivatedAt(LocalDateTime.now());
        contractImported.setContractType("type");

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        asserter.assertThat(() -> onboardingService.onboardingImport(request, List.of(), contractImported, false), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_aggregationCompletion(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_IO.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setInstitutionType(InstitutionType.SA);
        request.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        asserter.assertThat(() -> onboardingService.onboardingAggregationCompletion(request, users, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboardingCompletion(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        Product product = createDummyProduct(PROD_IO.getValue(), false);
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_IO.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setInstitutionType(InstitutionType.SA);
        request.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedMap(request.getInstitution().getTaxCode(), request.getProductId(), asserter);

        asserter.execute(() -> when(productService.getProduct(any())).thenReturn(product));

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        asserter.assertThat(() -> onboardingService.onboardingCompletion(request, users), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboardingUsers(UniAsserter asserter) {
        OnboardingUserRequest request = new OnboardingUserRequest();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        request.setUsers(users);
        request.setInstitutionType(InstitutionType.PA);
        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);

        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().item(onboarding));
        when(Onboarding.find((Document) any(), any())).thenReturn(query);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        org.openapi.quarkus.core_json.model.InstitutionResponse institutionResponse = new org.openapi.quarkus.core_json.model.InstitutionResponse();
        institutionResponse.setOrigin(Origin.IPA.name());
        institutionResponse.setOriginId("originId");
        InstitutionsResponse response = new InstitutionsResponse();
        response.setInstitutions(List.of(institutionResponse));
        asserter.execute(() -> when(institutionApi.getInstitutionsUsingGET(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(response)));

        asserter.assertThat(() -> onboardingService.onboardingUsers(request, "userId", WorkflowType.USERS), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
        });
    }

    @Test
    @RunOnVertxContext
    void onboardingUsersWithNullOnboardingReeferenceId(UniAsserter asserter) {
        OnboardingUserRequest request = new OnboardingUserRequest();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        request.setUsers(users);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter);

        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().nullItem());
        when(Onboarding.find((Document) any(), any())).thenReturn(query);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        org.openapi.quarkus.core_json.model.InstitutionResponse institutionResponse = new org.openapi.quarkus.core_json.model.InstitutionResponse();
        institutionResponse.setOrigin(Origin.IPA.name());
        institutionResponse.setOriginId("originId");
        InstitutionsResponse response = new InstitutionsResponse();
        response.setInstitutions(List.of(institutionResponse));
        when(institutionApi.getInstitutionsUsingGET(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(response));

        asserter.assertFailedWith(() -> onboardingService.onboardingUsers(request, "userId", WorkflowType.USERS_EA), ResourceNotFoundException.class);

    }

    @Test
    void onboardingUsersWithInstitutionNotFound() {
        OnboardingUserRequest request = new OnboardingUserRequest();
        List<UserRequest> users = List.of(manager);
        request.setTaxCode("taxCode");
        request.setSubunitCode("subunitCode");
        request.setProductId(PROD_INTEROP.getValue());
        request.setUsers(users);

        org.openapi.quarkus.core_json.model.InstitutionResponse institutionResponse = new org.openapi.quarkus.core_json.model.InstitutionResponse();
        institutionResponse.setOrigin(Origin.IPA.name());
        institutionResponse.setOriginId("originId");
        InstitutionsResponse response = new InstitutionsResponse();
        response.setInstitutions(List.of(institutionResponse, institutionResponse));
        when(institutionApi.getInstitutionsUsingGET("taxCode", "subunitCode", null, null))
                .thenReturn(Uni.createFrom().item(response));

        onboardingService
                .onboardingUsers(request, "userId", WorkflowType.USERS)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ResourceNotFoundException.class);

    }

    @Test
    void testInstitutionOnboardings() {
        Onboarding onboarding = mock(Onboarding.class);
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().item(onboarding));
        when(Onboarding.find(any())).thenReturn(query);
        UniAssertSubscriber<List<OnboardingResponse>> subscriber = onboardingService
                .institutionOnboardings("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.PENDING)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        List<OnboardingResponse> response = subscriber.assertCompleted().awaitItem().getItem();
        assertFalse(response.isEmpty());
        assertEquals(1, response.size());
    }

    @Test
    void testVerifyOnboardingNonEmptyList() {
        Onboarding onboarding = mock(Onboarding.class);
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().item(onboarding));
        when(Onboarding.find(any())).thenReturn(query);
        UniAssertSubscriber<List<OnboardingResponse>> subscriber = onboardingService
                .verifyOnboarding("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.COMPLETED, "prod-interop")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        List<OnboardingResponse> response = subscriber.assertCompleted().awaitItem().getItem();
        assertFalse(response.isEmpty());
        assertEquals(1, response.size());
    }

    @Test
    void checkRecipientCodeWithValidResponse() {
        String recipientCode = "recipientCode";
        String originId = "originId";
        CustomError customError = CustomError.DENIED_NO_ASSOCIATION;
        UOResource uoResource = Mockito.mock(UOResource.class);
        OnboardingUtils onboardingUtils = Mockito.mock(OnboardingUtils.class);
        // Mock the response from uoApi.findByUnicodeUsingGET1
        when(uoApi.findByUnicodeUsingGET1(eq(recipientCode), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        // Mock the response from onboardingUtils.validationRecipientCode
        when(onboardingUtils.getValidationRecipientCodeError(originId, uoResource))
                .thenReturn(Uni.createFrom().item(customError));

        // Call the method under test
        onboardingService
                .checkRecipientCode(recipientCode, originId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(customError);
    }

    @Test
    void checkRecipientCodeWithGenericError() {
        final String recipientCode = "recipientCode";
        final String originId = "originId";
        RuntimeException exception = new RuntimeException("Generic error");
        when(uoApi.findByUnicodeUsingGET1(eq(recipientCode), any()))
                .thenReturn(Uni.createFrom().failure(exception));
        UniAssertSubscriber<CustomError> subscriber = onboardingService
                .checkRecipientCode(recipientCode, originId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(RuntimeException.class);
    }

    @Test
    void checkRecipientCodeWithResourceNotFound() {
        final String recipientCode = "recipientCode";
        final String originId = "originId";
        ClientWebApplicationException exception = new ClientWebApplicationException(HttpStatus.SC_NOT_FOUND);
        when(uoApi.findByUnicodeUsingGET1(eq(recipientCode), any()))
                .thenReturn(Uni.createFrom().failure(exception));
        UniAssertSubscriber<CustomError> subscriber = onboardingService
                .checkRecipientCode(recipientCode, originId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class);
    }

    @Nested
    @TestProfile(OnboardingTestProfile.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class OnboardingServiceDefaultWithSignatureValidationTest {
        // can't be tested
        //@Test
        @RunOnVertxContext
        void complete_shouldThrowExceptionWhenSignatureFail(UniAsserter asserter) {
            Onboarding onboarding = createDummyOnboarding();
            asserter.execute(() -> PanacheMock.mock(Onboarding.class));
            asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                    .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

            mockFindToken(asserter, onboarding.getId());

            //Mock find managerUserfiscal code
            String actualUseUid = onboarding.getUsers().get(0).getId();
            UserResource actualUserResource = new UserResource();
            actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
            asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                    .thenReturn(Uni.createFrom().item(actualUserResource)));

            //Mock contract signature fail
            asserter.execute(() -> doThrow(InvalidRequestException.class)
                    .when(signatureService)
                    .verifySignature(any(), any(), any()));

            asserter.assertFailedWith(() -> onboardingService.complete(onboarding.getId(), TEST_FORM_ITEM),
                    InvalidRequestException.class);
        }

        // can't be tested
        //@Test
        @RunOnVertxContext
        void completeOnboardingUsers_shouldThrowExceptionWhenSignatureFail(UniAsserter asserter) {
            Onboarding onboarding = createDummyUsersOnboarding();
            asserter.execute(() -> PanacheMock.mock(Onboarding.class));
            asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                    .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

            mockFindToken(asserter, onboarding.getId());

            //Mock find managerUserfiscal code
            String actualUseUid = onboarding.getUsers().get(0).getId();
            UserResource actualUserResource = new UserResource();
            actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
            asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                    .thenReturn(Uni.createFrom().item(actualUserResource)));

            //Mock contract signature fail
            asserter.execute(() -> doThrow(InvalidRequestException.class)
                    .when(signatureService)
                    .verifySignature(any(), any(), any()));

            asserter.assertFailedWith(() -> onboardingService.completeOnboardingUsers(onboarding.getId(), TEST_FORM_ITEM),
                    InvalidRequestException.class);
        }

        @Test
        @RunOnVertxContext
        void completeOnboardingUsers(UniAsserter asserter) {
            Onboarding onboarding = createDummyUsersOnboarding();
            onboarding.setProductId("productParentId");
            asserter.execute(() -> PanacheMock.mock(Onboarding.class));
            asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                    .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

            mockFindToken(asserter, onboarding.getId());

            //Mock find managerUserfiscal code
            String actualUseUid = onboarding.getUsers().get(0).getId();
            UserResource actualUserResource = new UserResource();
            actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
            asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                    .thenReturn(Uni.createFrom().item(actualUserResource)));

            //Mock contract signature fail
            asserter.execute(() -> doNothing()
                    .when(signatureService)
                    .verifySignature(any(), any(), any()));

            mockVerifyAllowedMap(onboarding.getInstitution().getTaxCode(), onboarding.getProductId(), asserter);
            mockSimpleProductValidAssert(onboarding.getProductId(), true, asserter);
            mockVerifyOnboardingNotFound();

            final String filepath = "upload-file-path";
            when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);
            mockUpdateToken(asserter, filepath);

            asserter.assertThat(() -> onboardingService.completeOnboardingUsers(onboarding.getId(), TEST_FORM_ITEM),
                    Assertions::assertNotNull);
        }

        @Test
        @RunOnVertxContext
        void complete(UniAsserter asserter) {
            Onboarding onboarding = createDummyOnboarding();
            asserter.execute(() -> PanacheMock.mock(Onboarding.class));
            asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                    .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

            mockFindToken(asserter, onboarding.getId());

            //Mock find managerUserfiscal code
            String actualUseUid = onboarding.getUsers().get(0).getId();
            UserResource actualUserResource = new UserResource();
            actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
            asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                    .thenReturn(Uni.createFrom().item(actualUserResource)));

            //Mock contract signature fail
            asserter.execute(() -> doNothing()
                    .when(signatureService)
                    .verifySignature(any(), any(), any()));

            mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter);
            mockVerifyOnboardingNotFound();
            mockVerifyAllowedMap(onboarding.getInstitution().getTaxCode(), onboarding.getProductId(), asserter);

            final String filepath = "upload-file-path";
            when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);
            mockUpdateToken(asserter, filepath);

            asserter.assertThat(() -> onboardingService.complete(onboarding.getId(), TEST_FORM_ITEM),
                    Assertions::assertNotNull);
        }
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

    void mockVerifyAllowedMap(String taxCode, String productId, UniAsserter asserter) {
        asserter.execute(() -> when(onboardingValidationStrategy.validate(productId, taxCode))
                .thenReturn(true));
    }

    private void mockUpdateOnboardingInfo(String onboardingId, Long updatedItemCount) {
        ReactivePanacheUpdate query = mock(ReactivePanacheUpdate.class);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.update(any(Document.class))).thenReturn(query);
        when(query.where("_id", onboardingId)).thenReturn(Uni.createFrom().item(updatedItemCount));
    }

    @Test
    void testUpdateOnboardingStatusOK() {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);

        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));

        mockUpdateOnboardingInfo(onboarding.getId(), 1L);
        UniAssertSubscriber<Long> subscriber = onboardingService
                .updateOnboarding(onboarding.getId(), onboarding)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(1L);
    }

    @Test
    void testOnboardingUpdateOnboardingNotFound() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));
        mockUpdateOnboardingInfo(onboarding.getId(), 0L);

        UniAssertSubscriber<Long> subscriber = onboardingService
                .updateOnboarding(onboarding.getId(), onboarding)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void testCheckManager() {
        OnboardingUserRequest request = createDummyUserRequest();
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().items(onboarding));
        when(Onboarding.find((Document) any(), any())).thenReturn(query);
        UserRequest userRequest = new UserRequest();
        userRequest.setRole(PartyRole.MANAGER);
        userRequest.setTaxCode("managerTaxCode");
        request.setUsers(List.of(userRequest));

        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionApi.retrieveUserInstitutions(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(List.of(new UserInstitutionResponse())));

        Uni<CheckManagerResponse> result = onboardingService.checkManager(request);
        CheckManagerResponse checkResponse = result.await().indefinitely();

        assertNotNull(checkResponse);
    }

    @Test
    void testCheckManagerWith404FromUserRegistry() {
        OnboardingUserRequest request = createDummyUserRequest();
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().items(onboarding));
        when(Onboarding.find((Document) any(), any())).thenReturn(query);

        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(exception.getResponse()).thenReturn(response);
        when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().failure(exception));

        UniAssertSubscriber<CheckManagerResponse> subscriber = onboardingService
                .checkManager(request)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        CheckManagerResponse checkResponse = subscriber.getItem();
        assertNotNull(checkResponse);

        verify(userRegistryApi).searchUsingPOST(any(), any());
    }

    @Test
    void testCheckManagerWith500FromUserRegistry() {
        OnboardingUserRequest request = createDummyUserRequest();
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().items(onboarding));
        when(Onboarding.find((Document) any(), any())).thenReturn(query);

        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(exception.getResponse()).thenReturn(response);
        when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().failure(exception));

        UniAssertSubscriber<CheckManagerResponse> subscriber = onboardingService
                .checkManager(request)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(WebApplicationException.class);

        verify(userRegistryApi).searchUsingPOST(any(), any());
    }

    @Test
    void testCheckManagerWithTrueCheck() {
        final UUID uuid = UUID.randomUUID();
        OnboardingUserRequest request = createDummyUserRequest();
        Onboarding onboarding = createDummyOnboarding();
        onboarding.getUsers().get(0).setId(uuid.toString());
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().items(onboarding));
        when(Onboarding.find((Document) any(), any())).thenReturn(query);
        UserResource userResource = new UserResource();
        userResource.setId(uuid);
        when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        UserInstitutionResponse userInstitutionResponse = new UserInstitutionResponse();
        when(userInstitutionApi.retrieveUserInstitutions(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(List.of(userInstitutionResponse)));

        UniAssertSubscriber<CheckManagerResponse> subscriber = onboardingService
                .checkManager(request)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        CheckManagerResponse response = subscriber.getItem();
        assertNotNull(response);
        verify(userRegistryApi).searchUsingPOST(any(), any());
    }

    @Test
    void testInvalidRequestExceptionWhenManagerIsNull() {
        OnboardingUserRequest request = new OnboardingUserRequest();
        request.setUsers(Collections.emptyList());

        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> onboardingService.checkManager(request));
        assertEquals("At least one user should have role MANAGER", exception.getMessage());
    }

    @Test
    void testCheckManagerWithEmptyOnboardings() {
        OnboardingUserRequest request = createDummyUserRequest();
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().empty());
        when(Onboarding.find((Document) any(), any())).thenReturn(query);
        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<CheckManagerResponse> subscriber = onboardingService
                .checkManager(request)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        CheckManagerResponse checkResponse = subscriber.getItem();
        assertNotNull(checkResponse);
        assertFalse(checkResponse.isResponse());
    }

    @Test
    void testCheckManagerWithEmptyUserList() {
        final UUID uuid = UUID.randomUUID();
        OnboardingUserRequest request = createDummyUserRequest();
        Onboarding onboarding = createDummyOnboarding();
        onboarding.getUsers().get(0).setId(uuid.toString());
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().items(onboarding));
        when(Onboarding.find((Document) any(), any())).thenReturn(query);
        UserResource userResource = new UserResource();
        userResource.setId(uuid);
        when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        when(userInstitutionApi.retrieveUserInstitutions(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        UniAssertSubscriber<CheckManagerResponse> subscriber = onboardingService
                .checkManager(request)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        CheckManagerResponse response = subscriber.getItem();
        assertNotNull(response);
        assertFalse(response.isResponse());
        verify(userRegistryApi).searchUsingPOST(any(), any());
        verify(userInstitutionApi).retrieveUserInstitutions(any(), any(), any(), any(), any(), any());
    }

    private static OnboardingUserRequest createDummyUserRequest() {
        OnboardingUserRequest request = new OnboardingUserRequest();
        UserRequest user = new UserRequest();
        user.setTaxCode("taxCode");
        user.setRole(PartyRole.MANAGER);
        request.setUsers(List.of(user));
        return request;
    }

    @Test
    @RunOnVertxContext
    void testOnboardingUserPgFailsWhenUserListIsWrong(UniAsserter asserter) {
        assertThrows(InvalidRequestException.class, () -> onboardingService.onboardingUserPg(new Onboarding(), new ArrayList<>()));
    }

    @Test
    @RunOnVertxContext
    void testOnboardingUserPgFailsWhenInstitutionWasNotPreviouslyOnboarded(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        List<UserRequest> userRequests = List.of(manager);

        asserter.execute(() -> {
            PanacheMock.mock(Onboarding.class);
            ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
            when(query.stream()).thenReturn(Multi.createFrom().empty());
            when(Onboarding.find((Document) any(), any())).thenReturn(query);
        });

        asserter.assertFailedWith(() -> onboardingService.onboardingUserPg(onboarding, userRequests), ResourceNotFoundException.class);
    }

    @Test
    @RunOnVertxContext
    void testOnboardingUserPgFailsWhenUserWasAlreadyManager(UniAsserter asserter) {
        Onboarding previousOnboarding = createDummyOnboarding();
        previousOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);

        Onboarding newOnboarding = createDummyOnboarding();
        newOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);

        List<UserRequest> userRequests = List.of(manager);

        mockFindOnboarding(asserter, previousOnboarding);
        mockSimpleProductValidAssert(newOnboarding.getProductId(), false, asserter);
        mockSimpleSearchPOSTAndPersist(asserter);

        mockRetrieveUserInstitutions(newOnboarding.getInstitution().getId(), true, asserter);

        asserter.assertFailedWith(() -> onboardingService.onboardingUserPg(newOnboarding, userRequests), InvalidRequestException.class);

    }

    @Test
    @RunOnVertxContext
    void testOnboardingUserPgFailsWhenUserIsNotManagerOnInfocamereRegistry(UniAsserter asserter) {
        Onboarding previousOnboarding = createDummyOnboarding();
        previousOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);
        previousOnboarding.getInstitution().setOrigin(Origin.INFOCAMERE);

        Onboarding newOnboarding = createDummyOnboarding();
        newOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);
        newOnboarding.getInstitution().setOrigin(Origin.INFOCAMERE);

        List<UserRequest> userRequests = List.of(manager);

        mockFindOnboarding(asserter, previousOnboarding);
        mockSimpleProductValidAssert(newOnboarding.getProductId(), false, asserter);
        mockSimpleSearchPOSTAndPersist(asserter);

        mockRetrieveUserInstitutions(newOnboarding.getInstitution().getId(), false, asserter);

        asserter.execute(() -> when(infocamereApi.institutionsByLegalTaxIdUsingPOST(any()))
                .thenReturn(Uni.createFrom().item(new BusinessesResource())));

        asserter.assertFailedWith(() -> onboardingService.onboardingUserPg(newOnboarding, userRequests), InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void testOnboardingUserPgFailsWhenUserIsNotManagerOnAdeRegistry(UniAsserter asserter) {
        Onboarding previousOnboarding = createDummyOnboarding();
        previousOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);
        previousOnboarding.getInstitution().setOrigin(Origin.ADE);

        Onboarding newOnboarding = createDummyOnboarding();
        newOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);
        newOnboarding.getInstitution().setOrigin(Origin.ADE);

        List<UserRequest> userRequests = List.of(manager);

        mockFindOnboarding(asserter, previousOnboarding);
        mockSimpleProductValidAssert(newOnboarding.getProductId(), false, asserter);
        mockSimpleSearchPOSTAndPersist(asserter);

        mockRetrieveUserInstitutions(newOnboarding.getInstitution().getId(), false, asserter);

        LegalVerificationResult legalVerificationResult = new LegalVerificationResult();
        legalVerificationResult.setVerificationResult(false);

        asserter.execute(() -> when(nationalRegistriesApi.verifyLegalUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(legalVerificationResult)));

        asserter.assertFailedWith(() -> onboardingService.onboardingUserPg(newOnboarding, userRequests), InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void testOnboardingUserPg(UniAsserter asserter) {
        Onboarding previousOnboarding = createDummyOnboarding();
        previousOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);
        previousOnboarding.getInstitution().setOrigin(Origin.ADE);

        Onboarding newOnboarding = createDummyOnboarding();
        newOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);
        newOnboarding.getInstitution().setOrigin(Origin.ADE);

        List<UserRequest> userRequests = List.of(manager);

        mockFindOnboarding(asserter, previousOnboarding);
        mockSimpleProductValidAssert(newOnboarding.getProductId(), false, asserter);
        mockSimpleSearchPOSTAndPersist(asserter);

        mockRetrieveUserInstitutions(newOnboarding.getInstitution().getId(), false, asserter);

        LegalVerificationResult legalVerificationResult = new LegalVerificationResult();
        legalVerificationResult.setVerificationResult(true);

        asserter.execute(() -> when(nationalRegistriesApi.verifyLegalUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(legalVerificationResult)));

        asserter.execute(() -> when(Onboarding.persistOrUpdate(any(List.class)))
                .thenAnswer(arg -> {
                    List<Onboarding> onboardings = (List<Onboarding>) arg.getArguments()[0];
                    onboardings.get(0).setId(UUID.randomUUID().toString());
                    return Uni.createFrom().nullItem();
                }));

        asserter.execute(() -> when(orchestrationApi.apiStartOnboardingOrchestrationGet(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse())));

        asserter.assertNotNull(() -> onboardingService.onboardingUserPg(newOnboarding, userRequests));
    }

    private void mockRetrieveUserInstitutions(String institutionId, boolean shouldRetrieveUser, UniAsserter asserter) {
        UserInstitutionResponse userInstitutionResponse = new UserInstitutionResponse();
        userInstitutionResponse.setId("test");
        userInstitutionResponse.setInstitutionId(institutionId);

        asserter.execute(() -> when(userInstitutionApi.retrieveUserInstitutions(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(shouldRetrieveUser ? List.of(userInstitutionResponse) : Collections.emptyList())));
    }

    private void mockFindOnboarding(UniAsserter asserter, Onboarding onboarding) {
        asserter.execute(() -> {
            PanacheMock.mock(Onboarding.class);
            ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
            when(query.stream()).thenReturn(Multi.createFrom().item(onboarding));
            when(Onboarding.find((Document) any(), any())).thenReturn(query);
        });
    }
}
