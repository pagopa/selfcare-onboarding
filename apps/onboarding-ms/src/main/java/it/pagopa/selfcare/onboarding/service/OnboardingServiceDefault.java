package it.pagopa.selfcare.onboarding.service;

import io.quarkus.logging.Log;
import io.quarkus.mongodb.panache.common.reactive.Panache;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.unchecked.Unchecked;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.controller.request.AggregateInstitutionRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingImportContract;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingUserRequest;
import it.pagopa.selfcare.onboarding.controller.request.UserRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.controller.response.UserResponse;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceConflictException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import it.pagopa.selfcare.onboarding.service.strategy.OnboardingValidationStrategy;
import it.pagopa.selfcare.onboarding.service.util.OnboardingUtils;
import it.pagopa.selfcare.onboarding.util.InstitutionUtils;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import it.pagopa.selfcare.onboarding.util.SortEnum;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.PHASE_ADDITION_ALLOWED;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.api.OnboardingApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.core_json.model.InstitutionsResponse;
import org.openapi.quarkus.core_json.model.OnboardedProductResponse;
import org.openapi.quarkus.onboarding_functions_json.api.OrchestrationApi;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamereApi;
import org.openapi.quarkus.party_registry_proxy_json.api.NationalRegistriesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.BusinessesResource;
import org.openapi.quarkus.party_registry_proxy_json.model.GetInstitutionsByLegalDto;
import org.openapi.quarkus.party_registry_proxy_json.model.GetInstitutionsByLegalFilterDto;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.common.InstitutionType.PSP;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_INTEROP;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;
import static it.pagopa.selfcare.onboarding.constants.CustomError.*;
import static it.pagopa.selfcare.onboarding.util.ErrorMessage.*;
import static it.pagopa.selfcare.product.utils.ProductUtils.validRoles;

@ApplicationScoped
public class OnboardingServiceDefault implements OnboardingService {

    private static final Logger LOG = Logger.getLogger(OnboardingServiceDefault.class);
    protected static final String ATLEAST_ONE_PRODUCT_ROLE_REQUIRED =
            "At least one Product role related to %s Party role is required";
    protected static final String MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE =
            "More than one Product role related to %s Party role is available. Cannot automatically set the Product role";
    private static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE =
            "Institution with external id '%s' is not allowed to onboard '%s' product";
    private static final String ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED =
            "Onboarding with id %s not found or already deleted";
    private static final String GSP_CATEGORY_INSTITUTION_TYPE = "L37";
    public static final String
            UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED =
            "Unable to complete the onboarding for institution with taxCode '%s' to product '%s', the product is dismissed.";
    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
    public static final String USERS_FIELD_TAXCODE = "fiscalCode";
    public static final String TIMEOUT_ORCHESTRATION_RESPONSE = "65";
    private static final String ID_MAIL_PREFIX = "ID_MAIL#";
    public static final String NOT_MANAGER_OF_THE_INSTITUTION_ON_THE_REGISTRY =
            "User is not manager of the institution on the registry";

    @RestClient @Inject UserApi userRegistryApi;

    @RestClient @Inject OnboardingApi onboardingApi;

    @RestClient @Inject
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionRegistryProxyApi;

    @RestClient @Inject AooApi aooApi;

    @RestClient @Inject InstitutionApi institutionApi;

    @RestClient @Inject UoApi uoApi;

    @RestClient @Inject OrchestrationApi orchestrationApi;

    @RestClient @Inject InfocamereApi infocamereApi;

    @RestClient @Inject NationalRegistriesApi nationalRegistriesApi;

    @RestClient @Inject org.openapi.quarkus.user_json.api.InstitutionApi userInstitutionApi;

    @Inject OnboardingMapper onboardingMapper;

    @Inject InstitutionMapper institutionMapper;
    @Inject RegistryResourceFactory registryResourceFactory;
    @Inject OnboardingValidationStrategy onboardingValidationStrategy;
    @Inject ProductService productService;
    @Inject SignatureService signatureService;
    @Inject AzureBlobClient azureBlobClient;
    @Inject UserMapper userMapper;
    @Inject OnboardingUtils onboardingUtils;

    @ConfigProperty(name = "onboarding.expiring-date")
    Integer onboardingExpireDate;

    @ConfigProperty(name = "onboarding.orchestration.enabled")
    Boolean onboardingOrchestrationEnabled;

    @ConfigProperty(name = "onboarding-ms.signature.verify-enabled")
    Boolean isVerifyEnabled;

    @ConfigProperty(name = "onboarding-ms.blob-storage.path-contracts")
    String pathContracts;

    @Override
    public Uni<OnboardingResponse> onboarding(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates) {
        onboarding.setExpiringDate(
                OffsetDateTime.now().plusDays(onboardingExpireDate).toLocalDateTime());
        onboarding.setWorkflowType(getWorkflowType(onboarding));
        onboarding.setStatus(OnboardingStatus.REQUEST);

        return fillUsersAndOnboarding(onboarding, userRequests, aggregates, null, false);
    }

    @Override
    public Uni<OnboardingResponse> onboardingIncrement(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates) {
        onboarding.setExpiringDate(
                OffsetDateTime.now().plusDays(onboardingExpireDate).toLocalDateTime());
        onboarding.setWorkflowType(WorkflowType.INCREMENT_REGISTRATION_AGGREGATOR);
        onboarding.setStatus(OnboardingStatus.PENDING);

        return fillUsersAndOnboarding(onboarding, userRequests, aggregates, null, true);
    }

    /** As onboarding but it is specific for USERS workflow */
    @Override
    public Uni<OnboardingResponse> onboardingUsers(OnboardingUserRequest request, String userId, WorkflowType workflowType) {
        return getInstitutionFromUserRequest(request)
                .onItem()
                .transform(response -> institutionMapper.toEntity(response))
                .onItem()
                .transform(
                        institution -> {
                            Onboarding onboarding = onboardingMapper.toEntity(request, userId, workflowType);
                            institution.setInstitutionType(request.getInstitutionType());
                            onboarding.setInstitution(institution);
                            onboarding.setExpiringDate(
                                    OffsetDateTime.now().plusDays(onboardingExpireDate).toLocalDateTime());
                            return onboarding;
                        })
                .onItem()
                .transformToUni(onboarding -> fillUsers(onboarding, request.getUsers(), null));
    }

    /**
     * As above but it is specific for CONFIRMATION workflow where onboarding goes directly to persist
     * phase It is created with PENDING state and wait for completion of the orchestration of
     * persisting onboarding 'apiStartAndWaitOnboardingOrchestrationGet'
     */
    @Override
    public Uni<OnboardingResponse> onboardingCompletion(
            Onboarding onboarding, List<UserRequest> userRequests) {
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION);
        onboarding.setStatus(OnboardingStatus.PENDING);

        return fillUsersAndOnboarding(
                onboarding, userRequests, null, TIMEOUT_ORCHESTRATION_RESPONSE, false);
    }

    @Override
    public Uni<OnboardingResponse> onboardingAggregationCompletion(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates) {
        onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION_AGGREGATOR);
        onboarding.setStatus(OnboardingStatus.PENDING);

        return fillUsersAndOnboarding(onboarding, userRequests, aggregates, null, false);
    }

    /** As onboarding but it is specific for IMPORT workflow */
    @Override
    public Uni<OnboardingResponse> onboardingImport(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            OnboardingImportContract contractImported) {
        onboarding.setWorkflowType(WorkflowType.IMPORT);
        onboarding.setStatus(OnboardingStatus.PENDING);
        return fillUsersAndOnboardingForImport(
                onboarding, userRequests, contractImported, TIMEOUT_ORCHESTRATION_RESPONSE);
    }

    /**
     * @param timeout The orchestration instances will try complete within the defined timeout and the
     *     response is delivered synchronously. If is null the timeout is default 1 sec and the
     *     response is delivered asynchronously
     */
    private Uni<OnboardingResponse> fillUsersAndOnboarding(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates,
            String timeout,
            boolean isAggregatesIncrement) {
        onboarding.setCreatedAt(LocalDateTime.now());

        return getProductByOnboarding(onboarding)
                .onItem()
                .transformToUni(
                        product ->
                                verifyAlreadyOnboarding(
                                        onboarding.getInstitution(),
                                        product.getId(),
                                        product.getParentId(),
                                        isAggregatesIncrement)
                                        .replaceWith(product))
                .onItem().transformToUni(product -> Uni.createFrom().item(registryResourceFactory.create(onboarding))
                        .onItem().invoke(registryManager -> registryManager.setResource(registryManager.retrieveInstitution()))
                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                        .onItem().transformToUni(registryManager -> registryManager.isValid().onItem().transformToUni(ignored -> registryManager.customValidation(product)))
                                        /* if product has some test environments, request must also onboard them (for ex. prod-interop-coll) */
                                        .onItem()
                                        .invoke(() -> onboarding.setTestEnvProductIds(product.getTestEnvProductIds()))
                                        .onItem()
                                        .invoke(() -> onboarding.setTestEnvProductIds(product.getTestEnvProductIds()))
                                        .onItem()
                                        .transformToUni(
                                                current -> persistOnboarding(onboarding, userRequests, product, aggregates))
                                        /* Update onboarding data with users and start orchestration */
                                        .onItem()
                                        .transformToUni(
                                                currentOnboarding ->
                                                        persistAndStartOrchestrationOnboarding(
                                                                currentOnboarding,
                                                                orchestrationApi.apiStartOnboardingOrchestrationGet(
                                                                        currentOnboarding.getId(), timeout)))
                                        .onItem()
                                        .transform(onboardingMapper::toResponse));
    }

    /**
     * This method checks whether the product and any parent have already been onboarded for the
     * provided institution. In the case where we are in the aggregate increment flow, the product on
     * the aggregator entity must already be onboarded, so in the case of a ResourceConflictException,
     * the exception should not be propagated. In the case of standard onboarding, the exception
     * should be propagated, and the flow should be blocked.
     */
    private Uni<Void> verifyAlreadyOnboarding(
            Institution institution, String productId, String parentId, boolean isAggregatesIncrement) {
        if (isAggregatesIncrement) {
            return verifyAlreadyOnboardingForProductAndProductParent(institution, productId, parentId)
                    .onFailure(ResourceConflictException.class)
                    .recoverWithNull()
                    .replaceWithVoid();
        }
        return verifyAlreadyOnboardingForProductAndProductParent(institution, productId, parentId)
                .replaceWithVoid();
    }

    /**
     * @param timeout The orchestration instances will try complete within the defined timeout and the
     *     response is delivered synchronously. If is null the timeout is default 1 sec and the
     *     response is delivered asynchronously
     */
    private Uni<OnboardingResponse> fillUsers(
            Onboarding onboarding, List<UserRequest> userRequests, String timeout) {
        onboarding.setCreatedAt(LocalDateTime.now());

        return getProductByOnboarding(onboarding)
                .onItem()
                .transformToUni(
                        product ->
                                this.addReferencedOnboardingId(onboarding)
                                        /* if product has some test environments, request must also onboard them (for ex. prod-interop-coll) */
                                        .onItem()
                                        .invoke(
                                                current -> onboarding.setTestEnvProductIds(product.getTestEnvProductIds()))
                                        .onItem()
                                        .transformToUni(
                                                current -> persistOnboarding(onboarding, userRequests, product, null))
                                        /* Update onboarding data with users and start orchestration */
                                        .onItem()
                                        .transformToUni(
                                                currentOnboarding ->
                                                        persistAndStartOrchestrationOnboarding(
                                                                currentOnboarding,
                                                                orchestrationApi.apiStartOnboardingOrchestrationGet(
                                                                        currentOnboarding.getId(), timeout)))
                                        .onItem()
                                        .transform(onboardingMapper::toResponse));
    }

    /**
     * @param timeout The orchestration instances will try complete within the defined timeout and the
     *     response is delivered synchronously. If is null the timeout is default 1 sec and the
     *     response is delivered asynchronously
     */
    private Uni<OnboardingResponse> fillUsersAndOnboardingForImport(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            OnboardingImportContract contractImported,
            String timeout) {
        onboarding.setCreatedAt(LocalDateTime.now());

        return getProductByOnboarding(onboarding)
                .onItem()
                .transformToUni(
                        product ->
                                verifyAlreadyOnboardingForProductAndProductParent(
                                        onboarding.getInstitution(), product.getId(), product.getParentId())
                                        .replaceWith(product))
                .onItem().transformToUni(product -> Uni.createFrom().item(registryResourceFactory.create(onboarding))
                        .onItem().invoke(registryManager -> registryManager.setResource(registryManager.retrieveInstitution()))
                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                        .onItem().transformToUni(registryManager -> registryManager.isValid().onItem().transformToUni(ignored -> registryManager.customValidation(product)))
                                        /* if product has some test environments, request must also onboard them (for ex. prod-interop-coll) */
                                        .onItem()
                                        .invoke(() -> onboarding.setTestEnvProductIds(product.getTestEnvProductIds()))
                                        .onItem()
                                        .transformToUni(this::setInstitutionTypeAndBillingData)
                                        .onItem()
                                        .transformToUni(
                                                current -> persistOnboarding(onboarding, userRequests, product, null))
                                        .onItem()
                                        .call(
                                                onboardingPersisted ->
                                                        Panache.withTransaction(
                                                                () ->
                                                                        Token.persist(
                                                                                getToken(onboardingPersisted, product, contractImported))))
                                        /* Update onboarding data with users and start orchestration */
                                        .onItem()
                                        .transformToUni(
                                                currentOnboarding ->
                                                        persistAndStartOrchestrationOnboarding(
                                                                currentOnboarding,
                                                                orchestrationApi.apiStartOnboardingOrchestrationGet(
                                                                        currentOnboarding.getId(), timeout)))
                                        .onItem()
                                        .transform(onboardingMapper::toResponse));
    }

    private Uni<Onboarding> persistOnboarding(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            Product product,
            List<AggregateInstitutionRequest> aggregates) {

        Log.infof(
                "persist onboarding for: product %s, product parent %s",
                product.getId(), product.getParentId());

        Map<PartyRole, ProductRoleInfo> roleMappings =
                Objects.nonNull(product.getParent())
                        ? product
                        .getParent()
                        .getRoleMappings(onboarding.getInstitution().getInstitutionType().name())
                        : product.getRoleMappings(onboarding.getInstitution().getInstitutionType().name());

        /* I have to retrieve onboarding id for saving reference to pdv */
        return Panache.withTransaction(
                () ->
                        Onboarding.persist(onboarding)
                                .replaceWith(onboarding)
                                .onItem()
                                .transformToUni(
                                        onboardingPersisted ->
                                                validationRole(
                                                        userRequests,
                                                        validRoles(
                                                                product,
                                                                PHASE_ADDITION_ALLOWED.ONBOARDING,
                                                                onboarding.getInstitution().getInstitutionType()))
                                                        .onItem()
                                                        .transformToUni(
                                                                ignore ->
                                                                        validateUserAggregatesRoles(
                                                                                aggregates,
                                                                                validRoles(
                                                                                        product,
                                                                                        PHASE_ADDITION_ALLOWED.ONBOARDING,
                                                                                        onboarding.getInstitution().getInstitutionType())))
                                                        .onItem()
                                                        .transformToUni(
                                                                ignore ->
                                                                        retrieveAndSetUserAggregatesResources(
                                                                                onboardingPersisted, product, aggregates))
                                                        .onItem()
                                                        .transformToUni(
                                                                ignore -> retrieveUserResources(userRequests, roleMappings))
                                                        .onItem()
                                                        .invoke(onboardingPersisted::setUsers)
                                                        .replaceWith(onboardingPersisted)));
    }

    private Uni<Onboarding> addReferencedOnboardingId(Onboarding onboarding) {
        final String taxCode = onboarding.getInstitution().getTaxCode();
        final String origin = onboarding.getInstitution().getOrigin().name();
        final String originId = onboarding.getInstitution().getOriginId();
        final String productId = onboarding.getProductId();
        final String subunitCode = onboarding.getInstitution().getSubunitCode();
        Multi<Onboarding> onboardings =
                getOnboardingByFilters(taxCode, subunitCode, origin, originId, productId);
        Uni<Onboarding> current =
                onboardings
                        .filter(item -> Objects.isNull(item.getReferenceOnboardingId()))
                        .toUni()
                        .onItem()
                        .ifNull()
                        .failWith(
                                () ->
                                        new ResourceNotFoundException(
                                                String.format(
                                                        "Onboarding for taxCode %s, origin %s, originId %s, productId %s, subunitCode %s not found",
                                                        taxCode, origin, originId, productId, subunitCode)))
                        .invoke(
                                previousOnboarding ->
                                        onboarding.setReferenceOnboardingId(previousOnboarding.getId()));
        return current
                .onItem()
                .transformToUni(ignored -> onboardings.collect().first())
                .onItem()
                .invoke(
                        lastOnboarding -> {
                            String previousManagerId =
                                    lastOnboarding.getUsers().stream()
                                            .filter(user -> user.getRole().equals(PartyRole.MANAGER))
                                            .map(User::getId)
                                            .findFirst()
                                            .orElse(null);
                            onboarding.setPreviousManagerId(previousManagerId);
                        })
                .replaceWith(onboarding);
    }

    private Multi<Onboarding> getOnboardingByFilters(
            String taxCode, String subunitCode, String origin, String originId, String productId) {
        final Map<String, String> queryParameter =
                QueryUtils.createMapForInstitutionOnboardingsQueryParameter(
                        taxCode, subunitCode, origin, originId, OnboardingStatus.COMPLETED, productId);
        Document sort = QueryUtils.buildSortDocument(Onboarding.Fields.createdAt.name(), SortEnum.DESC);
        Document query = QueryUtils.buildQuery(queryParameter);
        return Onboarding.find(query, sort).stream();
    }

    public Uni<Onboarding> persistAndStartOrchestrationOnboarding(
            Onboarding onboarding, Uni<OrchestrationResponse> orchestration) {
        final List<Onboarding> onboardings = new ArrayList<>();
        onboardings.add(onboarding);

        Log.infof(
                "Persist onboarding and start orchestration %b for: taxCode %s, subunitCode %s, type %s",
                onboardingOrchestrationEnabled,
                onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(),
                onboarding.getInstitution().getInstitutionType());

        if (Boolean.TRUE.equals(onboardingOrchestrationEnabled)) {
            return Onboarding.persistOrUpdate(onboardings)
                    .onItem()
                    .transformToUni(saved -> orchestration)
                    .replaceWith(onboarding);
        } else {
            return Onboarding.persistOrUpdate(onboardings).replaceWith(onboarding);
        }
    }

    /**
     * Identify which workflow must be trigger during onboarding process. Each workflow consist of
     * different activities such as creating contract or sending appropriate mail. For more
     * information look at <a
     * href="https://pagopa.atlassian.net/wiki/spaces/SCP/pages/776339638/DR+-+Domain+Onboarding">...</a>
     *
     * @param onboarding actual onboarding request
     * @return WorkflowType
     */
    private WorkflowType getWorkflowType(Onboarding onboarding) {
        InstitutionType institutionType = onboarding.getInstitution().getInstitutionType();
        if (InstitutionType.PT.equals(institutionType)) {
            return WorkflowType.FOR_APPROVE_PT;
        }

        if (Objects.nonNull(onboarding.getIsAggregator())
                && onboarding.getIsAggregator().equals(Boolean.TRUE)) {
            return WorkflowType.CONTRACT_REGISTRATION_AGGREGATOR;
        }

        if (InstitutionType.PA.equals(institutionType)
                || isGspAndProdInterop(institutionType, onboarding.getProductId())
                || InstitutionType.SA.equals(institutionType)
                || InstitutionType.AS.equals(institutionType)
                || (InstitutionType.PRV.equals(institutionType)
                && !PROD_PAGOPA.getValue().equals(onboarding.getProductId()))) {
            return WorkflowType.CONTRACT_REGISTRATION;
        }

        if (InstitutionType.PG.equals(institutionType)) {
            return WorkflowType.CONFIRMATION;
        }

        return WorkflowType.FOR_APPROVE;
    }

    private boolean isGspAndProdInterop(InstitutionType institutionType, String productId) {
        return InstitutionType.GSP == institutionType && productId.equals(PROD_INTEROP.getValue());
    }

    private Uni<Product> product(String productId) {
        return Uni.createFrom()
                .item(() -> productService.getProductIsValid(productId))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Product> getProductByOnboarding(Onboarding onboarding) {

        /* retrieve product, if is not valid will throw OnboardingNotAllowedException */
        return product(onboarding.getProductId())
                .onFailure()
                .transform(
                        ex ->
                                new OnboardingNotAllowedException(
                                        String.format(
                                                UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED,
                                                onboarding.getInstitution().getTaxCode(),
                                                onboarding.getProductId()),
                                        DEFAULT_ERROR.getCode()));
    }

    private Uni<Boolean> verifyAlreadyOnboardingForProductAndProductParent(
            Institution institution, String productId, String productParentId) {
        return (Objects.nonNull(productParentId)
                // If product has parent, I must verify if onboarding is present for parent and child
                ? checkIfAlreadyOnboardingAndValidateAllowedMap(institution, productParentId)
                .onFailure(ResourceConflictException.class)
                .recoverWithUni(
                        ignore -> checkIfAlreadyOnboardingAndValidateAllowedMap(institution, productId))
                // If product is a root, I must only verify if onboarding for root
                : checkIfAlreadyOnboardingAndValidateAllowedMap(institution, productId));
    }

    private Uni<Boolean> verifyOnboardingNotExistForProductAndProductParent(
            Onboarding onboarding, String productId, String productParentId) {
        return (Objects.nonNull(productParentId)
                // If product has parent, I must verify if onboarding is present for parent and child
                ? checkIfOnboardingNotExistAndValidateAllowedMap(onboarding, productParentId)
                .onFailure(ResourceConflictException.class)
                .recoverWithUni(
                        ignore -> checkIfOnboardingNotExistAndValidateAllowedMap(onboarding, productId))
                // If product is a root, I must only verify if onboarding for root
                : checkIfOnboardingNotExistAndValidateAllowedMap(onboarding, productId));
    }

    private Uni<Boolean> validateAllowedMap(String taxCode, String subunitCode, String productId) {
        Log.infof(
                "Validating allowed map for: taxCode %s, subunitCode %s, product %s",
                taxCode, subunitCode, productId);

        if (!onboardingValidationStrategy.validate(productId, taxCode)) {
            return Uni.createFrom()
                    .failure(
                            new OnboardingNotAllowedException(
                                    String.format(ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE, taxCode, productId),
                                    DEFAULT_ERROR.getCode()));
        }

        return Uni.createFrom().item(Boolean.TRUE);
    }

    private Uni<Boolean> checkIfAlreadyOnboardingAndValidateAllowedMap(
            Institution institution, String productId) {
        return validateAllowedMap(institution.getTaxCode(), institution.getSubunitCode(), productId)
                .flatMap(
                        ignored -> {
                            String origin =
                                    institution.getOrigin() != null ? institution.getOrigin().getValue() : null;
                            return verifyOnboarding(
                                    institution.getTaxCode(),
                                    institution.getSubunitCode(),
                                    origin,
                                    institution.getOriginId(),
                                    OnboardingStatus.COMPLETED,
                                    productId)
                                    .flatMap(
                                            onboardingResponses ->
                                                    onboardingResponses.isEmpty()
                                                            ? Uni.createFrom().item(Boolean.TRUE)
                                                            : Uni.createFrom()
                                                            .failure(
                                                                    new ResourceConflictException(
                                                                            String.format(
                                                                                    PRODUCT_ALREADY_ONBOARDED.getMessage(),
                                                                                    productId,
                                                                                    institution.getTaxCode()),
                                                                            PRODUCT_ALREADY_ONBOARDED.getCode())));
                        });
    }

    private Uni<Boolean> checkIfOnboardingNotExistAndValidateAllowedMap(
            Onboarding onboarding, String productId) {
        return validateAllowedMap(
                onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(),
                productId)
                .flatMap(
                        ignored -> {
                            if (Objects.isNull(onboarding.getReferenceOnboardingId())) {
                                return Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        INVALID_REFERENCE_ONBORADING.getMessage(),
                                                        INVALID_REFERENCE_ONBORADING.getCode()));
                            }
                            return Onboarding.findByIdOptional(onboarding.getReferenceOnboardingId())
                                    .onItem()
                                    .transformToUni(
                                            opt ->
                                                    opt.map(Onboarding.class::cast)
                                                            .filter(
                                                                    referenceOnboarding ->
                                                                            referenceOnboarding
                                                                                    .getStatus()
                                                                                    .equals(OnboardingStatus.COMPLETED))
                                                            .map(referenceOnboarding -> Uni.createFrom().item(Boolean.TRUE))
                                                            .orElse(
                                                                    Uni.createFrom()
                                                                            .failure(
                                                                                    new InvalidRequestException(
                                                                                            String.format(
                                                                                                    PRODUCT_NOT_ONBOARDED.getMessage(),
                                                                                                    onboarding.getProductId(),
                                                                                                    onboarding.getInstitution().getTaxCode(),
                                                                                                    PRODUCT_NOT_ONBOARDED.getCode())))));
                        });
    }

    private String retrieveProductRole(
            UserRequest userInfo, Map<PartyRole, ProductRoleInfo> roleMappings) {
        try {
            if (Objects.isNull(roleMappings) || roleMappings.isEmpty())
                throw new IllegalArgumentException("Role mappings is required");

            if (Objects.isNull(roleMappings.get(userInfo.getRole())))
                throw new IllegalArgumentException(
                        String.format(ATLEAST_ONE_PRODUCT_ROLE_REQUIRED, userInfo.getRole()));
            if (Objects.isNull((roleMappings.get(userInfo.getRole()).getRoles())))
                throw new IllegalArgumentException(
                        String.format(ATLEAST_ONE_PRODUCT_ROLE_REQUIRED, userInfo.getRole()));
            if (roleMappings.get(userInfo.getRole()).getRoles().size() != 1)
                throw new IllegalArgumentException(
                        String.format(MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE, userInfo.getRole()));
            return roleMappings.get(userInfo.getRole()).getRoles().get(0).getCode();

        } catch (IllegalArgumentException e) {
            throw new OnboardingNotAllowedException(e.getMessage(), DEFAULT_ERROR.getCode());
        }
    }

    private Uni<List<UserRequest>> validationRole(
            List<UserRequest> users, List<PartyRole> validRoles) {

        List<UserRequest> usersNotValidRole =
                users.stream().filter(user -> !validRoles.contains(user.getRole())).toList();
        if (!usersNotValidRole.isEmpty()) {
            String usersNotValidRoleString =
                    usersNotValidRole.stream()
                            .map(user -> user.getRole().toString())
                            .collect(Collectors.joining(","));
            return Uni.createFrom()
                    .failure(
                            new InvalidRequestException(
                                    String.format(
                                            CustomError.ROLES_NOT_ADMITTED_ERROR.getMessage(), usersNotValidRoleString),
                                    CustomError.ROLES_NOT_ADMITTED_ERROR.getCode()));
        }

        return Uni.createFrom().item(users);
    }

    private Uni<Void> validateUserAggregatesRoles(
            List<AggregateInstitutionRequest> aggregates, List<PartyRole> validRoles) {
        LOG.debug("starting validateUserAggregatesRoles");
        if (!CollectionUtils.isEmpty(aggregates)) {
            return Multi.createFrom()
                    .iterable(aggregates)
                    .filter(aggregate -> !CollectionUtils.isEmpty(aggregate.getUsers()))
                    .onItem()
                    .invoke(
                            aggregate ->
                                    LOG.debugf("Validating role for users of aggregate: %s", aggregate.getTaxCode()))
                    .onItem()
                    .transformToUniAndMerge(
                            aggregate ->
                                    validationRole(aggregate.getUsers(), validRoles)
                                            .onFailure()
                                            .invoke(
                                                    throwable ->
                                                            LOG.error(
                                                                    "Error during validation role for aggregate: %s",
                                                                    aggregate.getTaxCode(), throwable)))
                    .collect()
                    .asList()
                    .replaceWithVoid();
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> retrieveAndSetUserAggregatesResources(
            Onboarding onboarding, Product product, List<AggregateInstitutionRequest> aggregates) {

        Log.infof(
                "Retrieving user resources for aggregates: product %s, product parent %s",
                product.getId(), product.getParentId());

        Map<PartyRole, ProductRoleInfo> roleMappings =
                Objects.nonNull(product.getParent())
                        ? product
                        .getParent()
                        .getRoleMappings(onboarding.getInstitution().getInstitutionType().name())
                        : product.getRoleMappings(onboarding.getInstitution().getInstitutionType().name());

        if (!CollectionUtils.isEmpty(aggregates)) {
            return Multi.createFrom()
                    .iterable(aggregates)
                    .filter(aggregate -> !CollectionUtils.isEmpty(aggregate.getUsers()))
                    .onItem()
                    .invoke(
                            aggregate ->
                                    LOG.debugf("Retrieving user resources for aggregate: %s", aggregate.getTaxCode()))
                    .onItem()
                    .transformToUni(
                            aggregate ->
                                    retrieveUserResources(aggregate.getUsers(), roleMappings)
                                            .onFailure()
                                            .invoke(
                                                    throwable ->
                                                            LOG.errorf(
                                                                    "Error during retrieving user resources for aggregate: %s",
                                                                    aggregate.getTaxCode(), throwable))
                                            .onItem()
                                            .invoke(users -> setUsersInAggregateToPersist(onboarding, aggregate, users)))
                    .concatenate()
                    .onItem()
                    .ignoreAsUni();
        }

        return Uni.createFrom().voidItem();
    }

    private static void setUsersInAggregateToPersist(
            Onboarding onboarding, AggregateInstitutionRequest aggregate, List<User> users) {
        onboarding.getAggregates().stream()
                .filter(
                        aggregateInstitution ->
                                aggregateInstitution.getTaxCode().equals(aggregate.getTaxCode()))
                .findAny()
                .ifPresent(aggregateInstitutionRequest -> aggregateInstitutionRequest.setUsers(users));
    }

    private Uni<List<User>> retrieveUserResources(
            List<UserRequest> users, Map<PartyRole, ProductRoleInfo> roleMappings) {

        return Multi.createFrom()
                .iterable(users)
                .onItem()
                .transformToUni(
                        user ->
                                userRegistryApi
                                        /* search user by tax code */
                                        .searchUsingPOST(
                                                USERS_FIELD_LIST, new UserSearchDto().fiscalCode(user.getTaxCode()))

                                        /* retrieve userId, if found will eventually update some fields */
                                        .onItem()
                                        .transformToUni(
                                                userResource -> {
                                                    Optional<String> optUserMailRandomUuid =
                                                            Optional.ofNullable(user.getEmail())
                                                                    .map(mail -> retrieveUserMailUuid(userResource, mail));
                                                    Optional<MutableUserFieldsDto> optUserFieldsDto =
                                                            toUpdateUserRequest(user, userResource, optUserMailRandomUuid);
                                                    return optUserFieldsDto
                                                            .map(
                                                                    userUpdateRequest ->
                                                                            userRegistryApi
                                                                                    .updateUsingPATCH(
                                                                                            userResource.getId().toString(), userUpdateRequest)
                                                                                    .replaceWith(userResource.getId()))
                                                            .orElse(Uni.createFrom().item(userResource.getId()))
                                                            .map(
                                                                    userResourceId ->
                                                                            User.builder()
                                                                                    .id(userResourceId.toString())
                                                                                    .role(user.getRole())
                                                                                    .userMailUuid(optUserMailRandomUuid.orElse(null))
                                                                                    .productRole(retrieveProductRole(user, roleMappings))
                                                                                    .build());
                                                })
                                        /* if not found 404, will create new user */
                                        .onFailure(WebApplicationException.class)
                                        .recoverWithUni(
                                                ex -> {
                                                    if (((WebApplicationException) ex).getResponse().getStatus() != 404) {
                                                        return Uni.createFrom().failure(ex);
                                                    }

                                                    String userMailRandomUuid =
                                                            ID_MAIL_PREFIX.concat(UUID.randomUUID().toString());
                                                    return userRegistryApi
                                                            .saveUsingPATCH(createSaveUserDto(user, userMailRandomUuid))
                                                            .onItem()
                                                            .transform(
                                                                    userId ->
                                                                            User.builder()
                                                                                    .id(userId.getId().toString())
                                                                                    .role(user.getRole())
                                                                                    .userMailUuid(userMailRandomUuid)
                                                                                    .productRole(retrieveProductRole(user, roleMappings))
                                                                                    .build());
                                                }))
                .concatenate()
                .collect()
                .asList();
    }

    private SaveUserDto createSaveUserDto(UserRequest model, String userMailRandomUuid) {
        SaveUserDto resource = new SaveUserDto();
        resource.setFiscalCode(model.getTaxCode());
        resource.setName(
                new CertifiableFieldResourceOfstring()
                        .value(model.getName())
                        .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        resource.setFamilyName(
                new CertifiableFieldResourceOfstring()
                        .value(model.getSurname())
                        .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));

        if (Objects.nonNull(userMailRandomUuid)) {
            WorkContactResource contact = new WorkContactResource();
            contact.setEmail(
                    new CertifiableFieldResourceOfstring()
                            .value(model.getEmail())
                            .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
            resource.setWorkContacts(Map.of(userMailRandomUuid, contact));
        }
        return resource;
    }

    private String retrieveUserMailUuid(UserResource foundUser, String userMail) {
        if (Objects.isNull(foundUser.getWorkContacts())) {
            return ID_MAIL_PREFIX.concat(UUID.randomUUID().toString());
        }

        return foundUser.getWorkContacts().entrySet().stream()
                .filter(
                        entry ->
                                Objects.nonNull(entry.getValue()) && Objects.nonNull(entry.getValue().getEmail()))
                .filter(entry -> entry.getValue().getEmail().getValue().equals(userMail))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(ID_MAIL_PREFIX.concat(UUID.randomUUID().toString()));
    }

    protected static Optional<MutableUserFieldsDto> toUpdateUserRequest(
            UserRequest user, UserResource foundUser, Optional<String> optUserMailRandomUuid) {
        Optional<MutableUserFieldsDto> mutableUserFieldsDto = Optional.empty();
        if (isFieldToUpdate(foundUser.getName(), user.getName())) {
            MutableUserFieldsDto dto = new MutableUserFieldsDto();
            dto.setName(
                    new CertifiableFieldResourceOfstring()
                            .value(user.getName())
                            .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
            mutableUserFieldsDto = Optional.of(dto);
        }
        if (isFieldToUpdate(foundUser.getFamilyName(), user.getSurname())) {
            MutableUserFieldsDto dto = mutableUserFieldsDto.orElseGet(MutableUserFieldsDto::new);
            dto.setFamilyName(
                    new CertifiableFieldResourceOfstring()
                            .value(user.getSurname())
                            .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
            mutableUserFieldsDto = Optional.of(dto);
        }

        if (optUserMailRandomUuid.isPresent()) {
            Optional<String> entryMail =
                    Objects.nonNull(foundUser.getWorkContacts())
                            ? foundUser.getWorkContacts().keySet().stream()
                            .filter(key -> key.equals(optUserMailRandomUuid.get()))
                            .findFirst()
                            : Optional.empty();

            if (entryMail.isEmpty()) {
                MutableUserFieldsDto dto = mutableUserFieldsDto.orElseGet(MutableUserFieldsDto::new);
                final WorkContactResource workContact = new WorkContactResource();
                workContact.setEmail(
                        new CertifiableFieldResourceOfstring()
                                .value(user.getEmail())
                                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
                dto.setWorkContacts(Map.of(optUserMailRandomUuid.get(), workContact));
                mutableUserFieldsDto = Optional.of(dto);
            }
        }
        return mutableUserFieldsDto;
    }

    private static boolean isFieldToUpdate(
            CertifiableFieldResourceOfstring certifiedField, String value) {
        boolean isToUpdate = true;
        if (certifiedField != null) {
            boolean isNoneCertification =
                    CertifiableFieldResourceOfstring.CertificationEnum.NONE.equals(
                            certifiedField.getCertification());
            boolean isSameValue =
                    isNoneCertification
                            ? certifiedField.getValue().equals(value)
                            : certifiedField.getValue().equalsIgnoreCase(value);

            if (isSameValue) {
                isToUpdate = false;
            } else if (!isNoneCertification) {
                throw new InvalidRequestException(
                        USERS_UPDATE_NOT_ALLOWED.getMessage(), USERS_UPDATE_NOT_ALLOWED.getCode());
            }
        }
        return isToUpdate;
    }

    @Override
    public Uni<OnboardingGet> approve(String onboardingId) {

        return retrieveOnboardingAndCheckIfExpired(onboardingId)
                .onItem()
                .transformToUni(this::checkIfToBeValidated)
                // Fail if onboarding exists for a product
                .onItem()
                .transformToUni(
                        onboarding ->
                                product(onboarding.getProductId())
                                        .onItem()
                                        .transformToUni(
                                                product ->
                                                        verifyAlreadyOnboardingForProductAndProductParent(
                                                                onboarding.getInstitution(),
                                                                product.getId(),
                                                                product.getParentId()))
                                        .replaceWith(onboarding))
                .onItem()
                .transformToUni(
                        onboarding ->
                                onboardingOrchestrationEnabled
                                        ? orchestrationApi
                                        .apiStartOnboardingOrchestrationGet(onboardingId, null)
                                        .map(ignore -> onboarding)
                                        : Uni.createFrom().item(onboarding))
                .map(onboardingMapper::toGetResponse);
    }

    @Override
    public Uni<Onboarding> complete(String onboardingId, FormItem formItem) {

        if (Boolean.TRUE.equals(isVerifyEnabled)) {
            // Retrieve as Tuple: managers fiscal-code from user registry and contract digest
            // At least, verify contract signature using both
            Function<Onboarding, Uni<Onboarding>> verification =
                    onboarding ->
                            Uni.combine()
                                    .all()
                                    .unis(
                                            retrieveOnboardingUserFiscalCodeList(onboarding),
                                            retrieveContractDigest(onboardingId))
                                    .asTuple()
                                    .onItem()
                                    .transformToUni(
                                            inputSignatureVerification ->
                                                    Uni.createFrom()
                                                            .item(
                                                                    () -> {
                                                                        signatureService.verifySignature(
                                                                                formItem.getFile(),
                                                                                inputSignatureVerification.getItem2(),
                                                                                inputSignatureVerification.getItem1());
                                                                        return onboarding;
                                                                    })
                                                            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()));
            return complete(onboardingId, formItem, verification);
        } else {
            return completeWithoutSignatureVerification(onboardingId, formItem);
        }
    }

    @Override
    public Uni<Onboarding> completeOnboardingUsers(String onboardingId, FormItem formItem) {
        if (Boolean.TRUE.equals(isVerifyEnabled)) {
            // Retrieve as Tuple: managers fiscal-code from user registry and contract digest
            // At least, verify contract signature using both
            Function<Onboarding, Uni<Onboarding>> verification =
                    onboarding ->
                            Uni.combine()
                                    .all()
                                    .unis(
                                            retrieveOnboardingUserFiscalCodeList(onboarding),
                                            retrieveContractDigest(onboardingId))
                                    .asTuple()
                                    .onItem()
                                    .transformToUni(
                                            inputSignatureVerification ->
                                                    Uni.createFrom()
                                                            .item(
                                                                    () -> {
                                                                        signatureService.verifySignature(
                                                                                formItem.getFile(),
                                                                                inputSignatureVerification.getItem2(),
                                                                                inputSignatureVerification.getItem1());
                                                                        return onboarding;
                                                                    })
                                                            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()));

            return completeOnboardingUsers(onboardingId, formItem, verification);
        } else {
            return completeOnboardingUsersWithoutSignatureVerification(onboardingId, formItem);
        }
    }

    public Uni<Onboarding> completeOnboardingUsersWithoutSignatureVerification(
            String onboardingId, FormItem formItem) {
        Function<Onboarding, Uni<Onboarding>> verification = ignored -> Uni.createFrom().item(ignored);
        return completeOnboardingUsers(onboardingId, formItem, verification);
    }

    @Override
    public Uni<Onboarding> completeWithoutSignatureVerification(
            String onboardingId, FormItem formItem) {
        Function<Onboarding, Uni<Onboarding>> verification = ignored -> Uni.createFrom().item(ignored);
        return complete(onboardingId, formItem, verification);
    }

    private Uni<Onboarding> complete(
            String onboardingId,
            FormItem formItem,
            Function<Onboarding, Uni<Onboarding>> verificationContractSignature) {

        return retrieveOnboardingAndCheckIfExpired(onboardingId)
                .onItem()
                .transformToUni(verificationContractSignature::apply)
                // Fail if onboarding exists for a product
                .onItem()
                .transformToUni(
                        onboarding ->
                                product(onboarding.getProductId())
                                        .onItem()
                                        .transformToUni(
                                                product ->
                                                        verifyAlreadyOnboardingForProductAndProductParent(
                                                                onboarding.getInstitution(),
                                                                product.getId(),
                                                                product.getParentId()))
                                        .replaceWith(onboarding))
                // Upload contract on storage
                .onItem()
                .transformToUni(
                        onboarding ->
                                uploadSignedContractAndUpdateToken(onboardingId, formItem)
                                        .map(ignore -> onboarding))
                // Start async activity if onboardingOrchestrationEnabled is true
                .onItem()
                .transformToUni(
                        onboarding ->
                                onboardingOrchestrationEnabled
                                        ? orchestrationApi
                                        .apiStartOnboardingOrchestrationGet(onboarding.getId(), null)
                                        .map(ignore -> onboarding)
                                        : Uni.createFrom().item(onboarding));
    }

    private Uni<Onboarding> completeOnboardingUsers(
            String onboardingId,
            FormItem formItem,
            Function<Onboarding, Uni<Onboarding>> verificationContractSignature) {

        return retrieveOnboardingAndCheckIfExpired(onboardingId)
                .onItem()
                .transformToUni(verificationContractSignature::apply)
                // Fail if onboarding exists for a product
                .onItem()
                .transformToUni(
                        onboarding ->
                                product(onboarding.getProductId())
                                        .onItem()
                                        .transformToUni(
                                                product ->
                                                        verifyOnboardingNotExistForProductAndProductParent(
                                                                onboarding, product.getId(), product.getParentId()))
                                        .replaceWith(onboarding))
                // Upload contract on storage
                .onItem()
                .transformToUni(
                        onboarding ->
                                uploadSignedContractAndUpdateToken(onboardingId, formItem)
                                        .map(ignore -> onboarding))
                // Start async activity if onboardingOrchestrationEnabled is true
                .onItem()
                .transformToUni(
                        onboarding ->
                                onboardingOrchestrationEnabled
                                        ? orchestrationApi
                                        .apiStartOnboardingOrchestrationGet(onboarding.getId(), null)
                                        .map(ignore -> onboarding)
                                        : Uni.createFrom().item(onboarding));
    }

    private Uni<String> uploadSignedContractAndUpdateToken(String onboardingId, FormItem formItem) {
        return retrieveToken(onboardingId)
                .onItem()
                .transformToUni(
                        token ->
                                Uni.createFrom()
                                        .item(
                                                Unchecked.supplier(
                                                        () -> {
                                                            final String path =
                                                                    String.format("%s%s", pathContracts, onboardingId);
                                                            final String signedContractExtension =
                                                                    getFileExtension(formItem.getFileName());
                                                            final String persistedContractFileName =
                                                                    Optional.ofNullable(token.getContractFilename())
                                                                            .orElse(onboardingId);
                                                            final String signedContractFileName =
                                                                    replaceFileExtension(
                                                                            persistedContractFileName, signedContractExtension);
                                                            final String filename =
                                                                    String.format("signed_%s", signedContractFileName);

                                                            try {
                                                                return azureBlobClient.uploadFile(
                                                                        path,
                                                                        filename,
                                                                        Files.readAllBytes(formItem.getFile().toPath()));
                                                            } catch (IOException e) {
                                                                throw new OnboardingNotAllowedException(
                                                                        GENERIC_ERROR.getCode(),
                                                                        "Error on upload contract for onboarding with id "
                                                                                + onboardingId);
                                                            }
                                                        }))
                                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                                        .onItem()
                                        .transformToUni(
                                                filepath ->
                                                        Token.update("contractSigned", filepath)
                                                                .where("_id", token.getId())
                                                                .replaceWith(filepath)));
    }

    private String getFileExtension(String name) {
        String[] parts = name.split("\\.");
        String ext = "";

        if (parts.length == 2) {
            return parts[1];
        }

        if (parts.length > 2) {
            // join all parts except the first one
            ext = String.join(".", Arrays.copyOfRange(parts, 1, parts.length));
        }

        return ext;
    }

    private String replaceFileExtension(String originalFilename, String newExtension) {
        int lastIndexOf = originalFilename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return originalFilename + newExtension;
        } else {
            return originalFilename.substring(0, lastIndexOf) + "." + newExtension;
        }
    }

    private Uni<Onboarding> retrieveOnboardingAndCheckIfExpired(String onboardingId) {
        // Retrieve Onboarding if exists
        return Onboarding.findByIdOptional(onboardingId)
                .onItem()
                .transformToUni(
                        opt ->
                                opt
                                        // I must cast to Onboarding because findByIdOptional return a generic
                                        // ReactiveEntity
                                        .map(Onboarding.class::cast)
                                        // Check if onboarding is expired
                                        .filter(onboarding -> !isOnboardingExpired(onboarding.getExpiringDate()))
                                        .map(onboarding -> Uni.createFrom().item(onboarding))
                                        .orElse(
                                                Uni.createFrom()
                                                        .failure(
                                                                new InvalidRequestException(
                                                                        String.format(
                                                                                ONBOARDING_EXPIRED.getMessage(),
                                                                                onboardingId,
                                                                                ONBOARDING_EXPIRED.getCode())))));
    }

    private Uni<Onboarding> checkIfToBeValidated(Onboarding onboarding) {
        return OnboardingStatus.TOBEVALIDATED.equals(onboarding.getStatus())
                ? Uni.createFrom().item(onboarding)
                : Uni.createFrom()
                .failure(
                        new InvalidRequestException(
                                String.format(
                                        ONBOARDING_NOT_TO_BE_VALIDATED.getMessage(),
                                        onboarding.getId(),
                                        ONBOARDING_NOT_TO_BE_VALIDATED.getCode())));
    }

    public static boolean isOnboardingExpired(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        return Objects.nonNull(dateTime) && (now.isEqual(dateTime) || now.isAfter(dateTime));
    }

    private Uni<String> retrieveContractDigest(String onboardingId) {
        return retrieveToken(onboardingId).map(Token::getChecksum);
    }

    private Uni<Token> retrieveToken(String onboardingId) {
        return Token.list("onboardingId", onboardingId)
                .map(tokens -> tokens.stream().findFirst().map(token -> (Token) token).orElseThrow());
    }

    private Uni<List<String>> retrieveOnboardingUserFiscalCodeList(Onboarding onboarding) {
        return Multi.createFrom()
                .iterable(
                        onboarding.getUsers().stream()
                                .filter(user -> PartyRole.MANAGER.equals(user.getRole()))
                                .map(User::getId)
                                .toList())
                .onItem()
                .transformToUni(userId -> userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, userId))
                .merge()
                .collect()
                .asList()
                .onItem()
                .transform(
                        usersResource -> usersResource.stream().map(UserResource::getFiscalCode).toList());
    }

    @Override
    public Uni<OnboardingGetResponse> onboardingGet(OnboardingGetFilters filters) {
        Document sort = QueryUtils.buildSortDocument(Onboarding.Fields.createdAt.name(), SortEnum.DESC);
        Map<String, String> queryParameter = QueryUtils.createMapForOnboardingQueryParameter(filters);
        Document query = QueryUtils.buildQuery(queryParameter);

        return Uni.combine()
                .all()
                .unis(
                        runQuery(query, sort).page(filters.getPage(), filters.getSize()).list(),
                        runQuery(query, null).count())
                .asTuple()
                .map(this::constructOnboardingGetResponse);
    }

    private ReactivePanacheQuery<Onboarding> runQuery(Document query, Document sort) {
        return Onboarding.find(query, sort);
    }

    private OnboardingGetResponse constructOnboardingGetResponse(
            Tuple2<List<Onboarding>, Long> tuple) {
        OnboardingGetResponse onboardingGetResponse = new OnboardingGetResponse();
        onboardingGetResponse.setCount(tuple.getItem2());
        onboardingGetResponse.setItems(convertOnboardingListToResponse(tuple.getItem1()));
        return onboardingGetResponse;
    }

    private List<OnboardingGet> convertOnboardingListToResponse(List<Onboarding> item1) {
        return item1.stream().map(onboardingMapper::toGetResponse).toList();
    }

    @Override
    public Uni<Long> rejectOnboarding(String onboardingId, String reasonForReject) {
        return Onboarding.findById(onboardingId)
                .onItem()
                .transform(Onboarding.class::cast)
                .onItem()
                .transformToUni(
                        onboardingGet ->
                                OnboardingStatus.COMPLETED.equals(onboardingGet.getStatus())
                                        ? Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        String.format("Onboarding with id %s is COMPLETED!", onboardingId)))
                                        : Uni.createFrom().item(onboardingGet))
                .onItem()
                .transformToUni(id -> updateReasonForRejectAndUpdateStatus(onboardingId, reasonForReject))
                // Start async activity if onboardingOrchestrationEnabled is true
                .onItem()
                .transformToUni(
                        onboarding ->
                                onboardingOrchestrationEnabled
                                        ? orchestrationApi
                                        .apiStartOnboardingOrchestrationGet(onboardingId, "60")
                                        .map(ignore -> onboarding)
                                        : Uni.createFrom().item(onboarding));
    }

    /**
     * Returns an onboarding record by its ID only if its status is PENDING. This feature is crucial
     * for ensuring that the onboarding process can be completed only when the onboarding status is
     * appropriately set to PENDING.
     *
     * @param onboardingId String
     * @return OnboardingGet
     */
    @Override
    public Uni<OnboardingGet> onboardingPending(String onboardingId) {
        return onboardingGet(onboardingId)
                .flatMap(
                        onboardingGet ->
                                OnboardingStatus.PENDING.name().equals(onboardingGet.getStatus())
                                        || OnboardingStatus.TOBEVALIDATED.name().equals(onboardingGet.getStatus())
                                        ? Uni.createFrom().item(onboardingGet)
                                        : Uni.createFrom()
                                        .failure(
                                                new ResourceNotFoundException(
                                                        String.format(
                                                                "Onboarding with id %s not found or not in PENDING status!",
                                                                onboardingId))));
    }

    @Override
    public Uni<List<OnboardingResponse>> institutionOnboardings(
            String taxCode, String subunitCode, String origin, String originId, OnboardingStatus status) {
        Map<String, String> queryParameter =
                QueryUtils.createMapForInstitutionOnboardingsQueryParameter(
                        taxCode, subunitCode, origin, originId, status, null);
        Document query = QueryUtils.buildQuery(queryParameter);
        return Onboarding.find(query).stream()
                .map(Onboarding.class::cast)
                .map(onboardingMapper::toResponse)
                .collect()
                .asList();
    }

    @Override
    public Uni<List<OnboardingResponse>> verifyOnboarding(
            String taxCode,
            String subunitCode,
            String origin,
            String originId,
            OnboardingStatus status,
            String productId) {
        Map<String, String> queryParameter =
                QueryUtils.createMapForInstitutionOnboardingsQueryParameter(
                        taxCode, subunitCode, origin, originId, status, productId);
        Document query = QueryUtils.buildQuery(queryParameter);
        return Onboarding.find(query).stream()
                .map(Onboarding.class::cast)
                .map(onboardingMapper::toResponse)
                .collect()
                .asList();
    }

    @Override
    public Uni<OnboardingGet> onboardingGet(String onboardingId) {
        return Onboarding.findByIdOptional(onboardingId)
                .onItem()
                .transformToUni(
                        opt ->
                                opt
                                        // I must cast to Onboarding because findByIdOptional return a generic
                                        // ReactiveEntity
                                        .map(Onboarding.class::cast)
                                        .map(onboardingMapper::toGetResponse)
                                        .map(onboardingGet -> Uni.createFrom().item(onboardingGet))
                                        .orElse(
                                                Uni.createFrom()
                                                        .failure(
                                                                new ResourceNotFoundException(
                                                                        String.format(
                                                                                "Onboarding with id %s not found!", onboardingId)))));
    }

    @Override
    public Uni<OnboardingGet> onboardingGetWithUserInfo(String onboardingId) {
        return Onboarding.findByIdOptional(onboardingId)
                .onItem()
                .transformToUni(
                        opt ->
                                opt
                                        // I must cast to Onboarding because findByIdOptional return a generic
                                        // ReactiveEntity
                                        .map(Onboarding.class::cast)
                                        .map(onboardingGet -> Uni.createFrom().item(onboardingGet))
                                        .orElse(
                                                Uni.createFrom()
                                                        .failure(
                                                                new ResourceNotFoundException(
                                                                        String.format(
                                                                                "Onboarding with id %s not found!", onboardingId)))))
                .flatMap(
                        onboarding ->
                                toUserResponseWithUserInfo(onboarding.getUsers())
                                        .onItem()
                                        .transform(
                                                userResponses -> {
                                                    OnboardingGet onboardingGet = onboardingMapper.toGetResponse(onboarding);
                                                    onboardingGet.setUsers(userResponses);
                                                    return onboardingGet;
                                                }));
    }

    private Uni<List<UserResponse>> toUserResponseWithUserInfo(List<User> users) {
        return Multi.createFrom()
                .iterable(users)
                .onItem()
                .transformToUni(
                        user ->
                                userRegistryApi
                                        .findByIdUsingGET(USERS_FIELD_LIST, user.getId())
                                        .onItem()
                                        .transform(
                                                userResource -> {
                                                    UserResponse userResponse = userMapper.toUserResponse(user);
                                                    userMapper.fillUserResponse(userResource, userResponse);

                                                    Optional.ofNullable(userResource.getWorkContacts())
                                                            .filter(map -> map.containsKey(user.getUserMailUuid()))
                                                            .map(map -> map.get(user.getUserMailUuid()))
                                                            .filter(
                                                                    workContract ->
                                                                            StringUtils.isNotBlank(workContract.getEmail().getValue()))
                                                            .map(workContract -> workContract.getEmail().getValue())
                                                            .ifPresent(userResponse::setEmail);
                                                    return userResponse;
                                                }))
                .merge()
                .collect()
                .asList();
    }

    private Uni<Onboarding> setInstitutionTypeAndBillingData(Onboarding onboarding) {
        if (Objects.nonNull(onboarding.getInstitution()) && !PSP.equals(onboarding.getInstitution().getInstitutionType())) {
            return institutionRegistryProxyApi.findInstitutionUsingGET(onboarding.getInstitution().getTaxCode(), null, null)
                    .onItem()
                    .invoke(proxyInstitution -> {
                        if (Objects.nonNull(proxyInstitution)) {
                            InstitutionType institutionType = proxyInstitution.getCategory().equalsIgnoreCase(GSP_CATEGORY_INSTITUTION_TYPE) ? InstitutionType.GSP : InstitutionType.PA;
                            onboarding.getInstitution().setInstitutionType(institutionType);

                            Billing billing = new Billing();
                            billing.setVatNumber(proxyInstitution.getTaxCode());
                            billing.setRecipientCode(proxyInstitution.getOriginId());
                            onboarding.setBilling(billing);
                        } else {
                            onboarding.getInstitution().setInstitutionType(InstitutionType.PA);
                        }
                    })
                    .replaceWith(Uni.createFrom().item(onboarding));
        }
        return Uni.createFrom().item(onboarding);
    }

    private static Uni<Long> updateReasonForRejectAndUpdateStatus(
            String onboardingId, String reasonForReject) {
        Map<String, Object> queryParameter =
                QueryUtils.createMapForOnboardingReject(reasonForReject, OnboardingStatus.REJECTED.name());
        Document query = QueryUtils.buildUpdateDocument(queryParameter);
        return Onboarding.update(query)
                .where("_id", onboardingId)
                .onItem()
                .transformToUni(
                        updateItemCount -> {
                            if (updateItemCount == 0) {
                                return Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        String.format(ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED, onboardingId)));
                            }
                            return Uni.createFrom().item(updateItemCount);
                        });
    }

    private Uni<InstitutionResponse> getInstitutionFromUserRequest(OnboardingUserRequest request) {
        Uni<InstitutionsResponse> responseUni;
        if (Objects.nonNull(request.getTaxCode()) && Objects.nonNull(request.getSubunitCode())) {
            responseUni =
                    institutionApi.getInstitutionsUsingGET(
                            request.getTaxCode(), request.getSubunitCode(), null, null);
        } else if (Objects.nonNull(request.getTaxCode())) {
            responseUni = institutionApi.getInstitutionsUsingGET(request.getTaxCode(), null, null, null);
        } else {
            responseUni =
                    institutionApi.getInstitutionsUsingGET(
                            null, null, request.getOrigin(), request.getOriginId());
        }
        return responseUni
                .onFailure(WebApplicationException.class)
                .recoverWithUni(
                        ex ->
                                ((WebApplicationException) ex).getResponse().getStatus() == 404
                                        ? Uni.createFrom()
                                        .failure(
                                                new ResourceNotFoundException(
                                                        String.format(
                                                                INSTITUTION_NOT_FOUND.getMessage(),
                                                                request.getTaxCode(),
                                                                request.getOrigin(),
                                                                request.getOriginId(),
                                                                request.getSubunitCode())))
                                        : Uni.createFrom().failure(ex))
                .onItem()
                .transformToUni(
                        response -> {
                            if (Objects.isNull(response.getInstitutions())
                                    || response.getInstitutions().size() > 1) {
                                return Uni.createFrom()
                                        .failure(
                                                new ResourceNotFoundException(
                                                        String.format(
                                                                INSTITUTION_NOT_FOUND.getMessage(),
                                                                request.getTaxCode(),
                                                                request.getOrigin(),
                                                                request.getOriginId(),
                                                                request.getSubunitCode())));
                            }
                            return Uni.createFrom().item(response.getInstitutions().get(0));
                        });
    }

    private Token getToken(
            Onboarding onboarding, Product product, OnboardingImportContract contractImported) {
        ContractTemplate contractTemplate =
                product.getInstitutionContractTemplate(
                        InstitutionUtils.getCurrentInstitutionType(onboarding));
        var token = new Token();
        token.setId(onboarding.getId());
        token.setOnboardingId(onboarding.getId());
        token.setContractTemplate(contractTemplate.getContractTemplatePath());
        token.setContractVersion(contractTemplate.getContractTemplateVersion());
        token.setContractSigned(contractImported.getFilePath());
        token.setContractFilename(contractImported.getFileName());
        token.setCreatedAt(contractImported.getCreatedAt());
        token.setUpdatedAt(contractImported.getCreatedAt());
        token.setProductId(onboarding.getProductId());
        token.setType(TokenType.INSTITUTION);
        return token;
    }

    @Override
    public Uni<Long> updateOnboarding(String onboardingId, Onboarding onboarding) {
        return Onboarding.findById(onboardingId)
                .onItem()
                .transform(Onboarding.class::cast)
                .onItem()
                .transformToUni(
                        onboardingGet ->
                                Objects.isNull(onboardingGet)
                                        ? Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        String.format(
                                                                "Onboarding with id %s is not present!", onboardingId)))
                                        : Uni.createFrom().item(onboardingGet))
                .onItem()
                .transformToUni(id -> updateOnboardingValues(onboardingId, onboarding));
    }

    @Override
    public Uni<CheckManagerResponse> checkManager(OnboardingUserRequest onboardingUserRequest) {
        CheckManagerResponse response = new CheckManagerResponse();

        String taxCodeManager =
                onboardingUserRequest.getUsers().stream()
                        .filter(user -> PartyRole.MANAGER == user.getRole())
                        .map(UserRequest::getTaxCode)
                        .findFirst()
                        .orElseThrow(
                                () -> new InvalidRequestException("At least one user should have role MANAGER"));

        return userRegistryApi
                .searchUsingPOST(USERS_FIELD_LIST, new UserSearchDto().fiscalCode(taxCodeManager))
                .onItem()
                .transform(UserResource::getId)
                .flatMap(
                        uuid ->
                                findOnboardingsByFilters(onboardingUserRequest)
                                        .flatMap(
                                                onboardings -> {
                                                    if (CollectionUtils.isEmpty(onboardings)) {
                                                        LOG.debugf(
                                                                "Onboarding for taxCode %s, origin %s, originId %s, productId %s, subunitCode %s not found",
                                                                onboardingUserRequest.getTaxCode(),
                                                                onboardingUserRequest.getOrigin(),
                                                                onboardingUserRequest.getOriginId(),
                                                                onboardingUserRequest.getProductId(),
                                                                onboardingUserRequest.getSubunitCode());

                                                        response.setResponse(false);
                                                        return Uni.createFrom().item(response);
                                                    }

                                                    String institutionId = onboardings.get(0).getInstitution().getId();
                                                    return isUserActiveManager(
                                                            institutionId,
                                                            onboardingUserRequest.getProductId(),
                                                            String.valueOf(uuid))
                                                            .map(
                                                                    isActiveManager -> {
                                                                        LOG.debugf(
                                                                                "User with uuid %s is active manager: %s",
                                                                                uuid, isActiveManager);
                                                                        response.setResponse(isActiveManager);
                                                                        return response;
                                                                    });
                                                }))
                .onFailure()
                .recoverWithUni(
                        ex -> {
                            if (ex instanceof WebApplicationException
                                    && ((WebApplicationException) ex).getResponse().getStatus() == 404) {
                                LOG.debugf("User not found on user-registry", taxCodeManager);
                                response.setResponse(false);
                                return Uni.createFrom().item(response);
                            }

                            // If the exception raised is for a different status code, let it propagate
                            return Uni.createFrom().failure(ex);
                        });
    }

    /**
     * Retrieves the onboarding record by the given filters.
     *
     * @param onboardingUserRequest OnboardingUserRequest
     * @return a Uni with the list of onboardings
     * @throws ResourceNotFoundException if the onboarding record is not found
     */
    private Uni<List<Onboarding>> findOnboardingsByFilters(
            OnboardingUserRequest onboardingUserRequest) {
        return getOnboardingByFilters(
                onboardingUserRequest.getTaxCode(),
                onboardingUserRequest.getSubunitCode(),
                onboardingUserRequest.getOrigin(),
                onboardingUserRequest.getOriginId(),
                onboardingUserRequest.getProductId())
                .collect()
                .asList();
    }

    /**
     * Checks if the user is an active manager within the institution for the given product invoking
     * selfcare-user API.
     *
     * @param institutionId institution id
     * @param productId product id
     * @param uuid user uuid
     * @return a Uni with the result of the check
     */
    private Uni<Boolean> isUserActiveManager(String institutionId, String productId, String uuid) {
        return userInstitutionApi
                .retrieveUserInstitutions(
                        institutionId,
                        null,
                        Objects.nonNull(productId) ? List.of(productId) : null,
                        List.of(String.valueOf(PartyRole.MANAGER)),
                        List.of(String.valueOf(OnboardedProductResponse.StatusEnum.ACTIVE)),
                        uuid)
                .onFailure()
                .invoke(e -> LOG.error("Error while checking if user is active manager", e))
                .onItem()
                .transform(CollectionUtils::isNotEmpty);
    }

    public Uni<CustomError> checkRecipientCode(String recipientCode, String originId) {
        return onboardingUtils
                .getUoFromRecipientCode(recipientCode)
                .onItem()
                .transformToUni(
                        uoResource -> onboardingUtils.getValidationRecipientCodeError(originId, uoResource));
    }

    private static Uni<Long> updateOnboardingValues(String onboardingId, Onboarding onboarding) {
        Map<String, Object> queryParameter = QueryUtils.createMapForOnboardingUpdate(onboarding);
        Document query = QueryUtils.buildUpdateDocument(queryParameter);
        return Onboarding.update(query)
                .where("_id", onboardingId)
                .onItem()
                .transformToUni(
                        updateItemCount -> {
                            if (updateItemCount == 0) {
                                return Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        String.format(ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED, onboardingId)));
                            }
                            return Uni.createFrom().item(updateItemCount);
                        });
    }

    private Uni<List<Onboarding>> getOnboardingList(List<Onboarding> onboardings) {
        if (onboardings.isEmpty()) {
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().item(onboardings);
    }

    /**
     * Initiates the onboarding process for a user in the PG (Persona Giuridica) context. This method
     * performs the following steps:
     *
     * <ul>
     *   <li>Validates the provided user requests to ensure only one manager is onboarded.
     *   <li>Sets the workflow type and status of the onboarding to USERS_PG and PENDING,
     *       respectively.
     *   <li>Retrieves any previous completed onboarding data for the institution and product.
     *   <li>Copies relevant data from the previous onboarding to the current onboarding instance.
     *   <li>Retrieves and sets the manager UID for the new onboarding.
     *   <li>Checks if the user is already a manager within the institution.
     *   <li>Verifies the manager's association with the institution in external registries
     *       (Infocamere or ADE).
     *   <li>Persists the onboarding data and initiates orchestration.
     * </ul>
     *
     * @param onboarding the onboarding data to process
     * @param userRequests the list of user requests associated with the onboarding
     * @return a Uni that emits the onboarding response upon successful completion
     * @throws InvalidRequestException if the user list is invalid or the user is already a manager
     * @throws ResourceNotFoundException if no previous onboarding data is found for the institution
     */
    public Uni<OnboardingResponse> onboardingUserPg(
            Onboarding onboarding, List<UserRequest> userRequests) {
        checkOnboardingPgUserList(userRequests);

        return retrievePreviousCompletedOnboarding(onboarding)
                .map(
                        previousOnboarding ->
                                copyDataFromPreviousToCurrentOnboarding(previousOnboarding, onboarding))
                .flatMap(unused -> retrieveAndSetManagerUidOnNewOnboarding(onboarding, userRequests))
                .flatMap(unused -> checkIfUserIsAlreadyManager(onboarding))
                .flatMap(unused -> checkIfUserIsManagerOnRegistries(onboarding, userRequests))
                .onItem()
                .transformToUni(
                        unused ->
                                persistAndStartOrchestrationOnboarding(
                                        onboarding,
                                        orchestrationApi.apiStartOnboardingOrchestrationGet(
                                                onboarding.getId(), TIMEOUT_ORCHESTRATION_RESPONSE)))
                .onItem()
                .transform(onboardingMapper::toResponse);
    }

    /**
     * Validates the list of user requests to ensure only one manager is present.
     *
     * @param userRequests the list of user requests to validate
     * @throws InvalidRequestException if the user list is empty, contains more than one user, or the
     *     user role is not MANAGER
     */
    private void checkOnboardingPgUserList(List<UserRequest> userRequests) {
        if (CollectionUtils.isEmpty(userRequests)
                || userRequests.size() > 1
                || !PartyRole.MANAGER.equals(userRequests.get(0).getRole())) {
            throw new InvalidRequestException(
                    "This API allows the onboarding of only one user with role MANAGER");
        }
    }

    private Uni<Onboarding> retrievePreviousCompletedOnboarding(Onboarding onboarding) {
        LOG.infof(
                "Retrieving previous completed onboarding for taxCode %s, origin %s, productId %s",
                onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getOrigin(),
                onboarding.getProductId());

        return getOnboardingByFilters(
                onboarding.getInstitution().getTaxCode(),
                null,
                String.valueOf(onboarding.getInstitution().getOrigin()),
                null,
                onboarding.getProductId())
                .collect()
                .asList()
                .onItem()
                .transformToUni(this::getOnboardingList)
                .onItem()
                .ifNull()
                .failWith(resourceNotFoundExceptionSupplier(onboarding))
                .map(
                        onboardings ->
                                onboardings.stream()
                                        .filter(o -> Objects.isNull(o.getReferenceOnboardingId()))
                                        .findFirst()
                                        .orElse(null));
    }

    private Supplier<ResourceNotFoundException> resourceNotFoundExceptionSupplier(
            Onboarding onboarding) {
        return () ->
                new ResourceNotFoundException(
                        String.format(
                                "Onboarding not found for taxCode %s, origin %s, productId %s",
                                onboarding.getInstitution().getTaxCode(),
                                onboarding.getInstitution().getOrigin(),
                                onboarding.getProductId()));
    }

    private Onboarding copyDataFromPreviousToCurrentOnboarding(
            Onboarding previousOnboarding, Onboarding currentOnboarding) {
        currentOnboarding.setReferenceOnboardingId(previousOnboarding.getId());
        currentOnboarding.setInstitution(previousOnboarding.getInstitution());
        return currentOnboarding;
    }

    private Uni<Onboarding> retrieveAndSetManagerUidOnNewOnboarding(
            Onboarding onboarding, List<UserRequest> userRequests) {
        return getProductByOnboarding(onboarding)
                .flatMap(
                        product ->
                                validationRole(
                                        userRequests,
                                        validRoles(
                                                product,
                                                PHASE_ADDITION_ALLOWED.ONBOARDING,
                                                onboarding.getInstitution().getInstitutionType()))
                                        .map(unused -> retrieveRoleMappingsFromProduct(product, onboarding)))
                .flatMap(roleMappings -> retrieveUserResources(userRequests, roleMappings))
                .onItem()
                .invoke(onboarding::setUsers)
                .replaceWith(onboarding);
    }

    private Map<PartyRole, ProductRoleInfo> retrieveRoleMappingsFromProduct(
            Product product, Onboarding onboarding) {
        return Objects.nonNull(product.getParent())
                ? product
                .getParent()
                .getRoleMappings(onboarding.getInstitution().getInstitutionType().name())
                : product.getRoleMappings(onboarding.getInstitution().getInstitutionType().name());
    }

    /**
     * Checks if the user is already a manager within the institution invoking selfcare-user API.
     *
     * @param currentOnboarding the current onboarding data
     * @return a Uni that completes if the user is not already a manager, otherwise fails
     * @throws InvalidRequestException if the user is already a manager of the institution
     */
    private Uni<Void> checkIfUserIsAlreadyManager(Onboarding currentOnboarding) {
        String newManagerId =
                currentOnboarding.getUsers().stream()
                        .filter(user -> PartyRole.MANAGER.equals(user.getRole()))
                        .map(User::getId)
                        .findAny()
                        .orElse(null);
        String institutionId = currentOnboarding.getInstitution().getId();

        LOG.infof(
                "Checking if user with id: %s is already manager of the institution with id: %s",
                newManagerId, institutionId);

        return isUserActiveManager(institutionId, currentOnboarding.getProductId(), newManagerId)
                .flatMap(
                        isActiveManager -> {
                            if (isActiveManager) {
                                throw new InvalidRequestException("User is already manager of the institution");
                            }
                            return Uni.createFrom().voidItem();
                        });
    }

    /**
     * Checks if the user is a manager in the external registries based on the institution's origin.
     *
     * @param onboarding the current onboarding data
     * @param userRequests the list of user requests associated with the onboarding
     * @return a Uni that completes if the user is a valid manager in the registry, otherwise fails
     * @throws InvalidRequestException if the user is not a manager in the external registry
     */
    private Uni<Void> checkIfUserIsManagerOnRegistries(
            Onboarding onboarding, List<UserRequest> userRequests) {
        LOG.infof(
                "Checking if user is manager on registries for onboarding with origin %s",
                onboarding.getInstitution().getOrigin());
        String userTaxCode =
                userRequests.stream()
                        .filter(userRequest -> PartyRole.MANAGER.equals(userRequest.getRole()))
                        .map(UserRequest::getTaxCode)
                        .findAny()
                        .orElse(null);

        String businessTaxCode = onboarding.getInstitution().getTaxCode();

        if (onboarding.getInstitution().getOrigin() == Origin.INFOCAMERE) {
            return checkIfUserIsManagerOnInfocamere(userTaxCode, businessTaxCode);
        } else {
            return checkIfUserIsManagerOnADE(userTaxCode, businessTaxCode);
        }
    }

    /**
     * Checks if the user is a manager in the Infocamere registry.
     *
     * @param userTaxCode the tax code of the user
     * @param businessTaxCode the tax code of the business (institution)
     * @return a Uni that completes if the user is a manager, otherwise fails
     * @throws InvalidRequestException if the user is not a manager in Infocamere
     */
    private Uni<Void> checkIfUserIsManagerOnInfocamere(String userTaxCode, String businessTaxCode) {
        return infocamereApi
                .institutionsByLegalTaxIdUsingPOST(toGetInstitutionsByLegalDto(userTaxCode))
                .flatMap(
                        businessesResource -> checkIfBusinessIsContained(businessesResource, businessTaxCode));
    }

    private GetInstitutionsByLegalDto toGetInstitutionsByLegalDto(String userTaxCode) {
        return GetInstitutionsByLegalDto.builder()
                .filter(GetInstitutionsByLegalFilterDto.builder().legalTaxId(userTaxCode).build())
                .build();
    }

    /**
     * Validates if the business tax code is contained within the retrieved businesses.
     *
     * @param businessesResource the resource containing businesses data
     * @param taxCode the tax code to validate against
     * @return a Uni that completes if the tax code is found, otherwise fails
     * @throws InvalidRequestException if the tax code is not found in the businesses resource
     */
    private Uni<Void> checkIfBusinessIsContained(
            BusinessesResource businessesResource, String taxCode) {
        if (Objects.isNull(businessesResource)
                || Objects.isNull(businessesResource.getBusinesses())
                || businessesResource.getBusinesses().stream()
                .noneMatch(business -> business.getBusinessTaxId().equals(taxCode))) {
            throw new InvalidRequestException(NOT_MANAGER_OF_THE_INSTITUTION_ON_THE_REGISTRY);
        }

        return Uni.createFrom().voidItem();
    }

    /**
     * Checks if the user is a manager in the ADE (Agenzia delle Entrate) registry.
     *
     * @param userTaxCode the tax code of the user
     * @param businessTaxCode the tax code of the business (institution)
     * @return a Uni that completes if the user is a manager, otherwise fails
     * @throws InvalidRequestException if the user is not a manager in ADE
     */
    private Uni<Void> checkIfUserIsManagerOnADE(String userTaxCode, String businessTaxCode) {
        return nationalRegistriesApi
                .verifyLegalUsingGET(userTaxCode, businessTaxCode)
                .onItem()
                .transformToUni(
                        legalVerificationResult -> {
                            if (!legalVerificationResult.getVerificationResult()) {
                                throw new InvalidRequestException(NOT_MANAGER_OF_THE_INSTITUTION_ON_THE_REGISTRY);
                            }
                            return Uni.createFrom().voidItem();
                        })
                .onFailure(WebApplicationException.class)
                .recoverWithUni(
                        ex -> {
                            // If the user is not manager of the institution, the response status code could be
                            // 400
                            if (((WebApplicationException) ex).getResponse().getStatus() == 400) {
                                return Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        "User is not manager of the institution on the registry"));
                            }

                            return Uni.createFrom().failure(ex);
                        });
    }
}