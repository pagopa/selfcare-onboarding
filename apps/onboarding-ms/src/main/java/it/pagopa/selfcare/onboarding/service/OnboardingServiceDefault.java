package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mongodb.panache.common.reactive.Panache;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.unchecked.Unchecked;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.controller.response.UserResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.service.strategy.OnboardingValidationStrategy;
import it.pagopa.selfcare.onboarding.util.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import it.pagopa.selfcare.onboarding.util.SortEnum;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.openapi.quarkus.core_json.api.OnboardingApi;
import org.openapi.quarkus.onboarding_functions_json.api.OrchestrationApi;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_INTEROP;
import static it.pagopa.selfcare.onboarding.constants.CustomError.*;
import static it.pagopa.selfcare.onboarding.util.GenericError.*;

@ApplicationScoped
public class OnboardingServiceDefault implements OnboardingService {
    protected static final String ATLEAST_ONE_PRODUCT_ROLE_REQUIRED = "At least one Product role related to %s Party role is required";
    protected static final String MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE = "More than one Product role related to %s Party role is available. Cannot automatically set the Product role";
    private static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE = "Institution with external id '%s' is not allowed to onboard '%s' product";
    private static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE = "Institution with external id '%s' is not allowed to onboard '%s' product because it is not delegable";
    private static final String INVALID_OBJECTID = "Given onboardingId [%s] has wrong format";
    private static final String ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED = "Onboarding with id %s not found or already deleted";
    public static final String UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED = "Unable to complete the onboarding for institution with taxCode '%s' to product '%s', the product is dismissed.";

    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
    public static final String USERS_FIELD_TAXCODE = "fiscalCode";
    public static final String UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_ALREADY_ONBOARDED = "Unable to complete the onboarding for institution with taxCode '%s' to product '%s' because is already onboarded.";


    public static final Function<String, String> workContactsKey = onboardingId -> String.format("obg_%s", onboardingId);

    @RestClient
    @Inject
    UserApi userRegistryApi;

    @RestClient
    @Inject
    OnboardingApi onboardingApi;

    @RestClient
    @Inject
    AooApi aooApi;

    @RestClient
    @Inject
    UoApi uoApi;

    @RestClient
    @Inject
    OrchestrationApi orchestrationApi;

    @Inject
    OnboardingMapper onboardingMapper;

    @Inject
    OnboardingValidationStrategy onboardingValidationStrategy;
    @Inject
    ProductService productService;
    @Inject
    SignatureService signatureService;
    @Inject
    AzureBlobClient azureBlobClient;

    @Inject
    UserMapper userMapper;

    @ConfigProperty(name = "onboarding.expiring-date")
    Integer onboardingExpireDate;
    @ConfigProperty(name = "onboarding.orchestration.enabled")
    Boolean onboardingOrchestrationEnabled;
    @ConfigProperty(name = "onboarding-ms.signature.verify-enabled")
    Boolean isVerifyEnabled;
    @ConfigProperty(name = "onboarding-ms.blob-storage.path-contracts")
    String pathContracts;

    @Override
    public Uni<OnboardingResponse> onboarding(OnboardingDefaultRequest onboardingRequest) {
        return fillUsersAndOnboarding(onboardingMapper.toEntity(onboardingRequest), onboardingRequest.getUsers());
    }

    @Override
    public Uni<OnboardingResponse> onboardingPsp(OnboardingPspRequest onboardingRequest) {
        return fillUsersAndOnboarding(onboardingMapper.toEntity(onboardingRequest), onboardingRequest.getUsers());
    }

    @Override
    public Uni<OnboardingResponse> onboardingSa(OnboardingSaRequest onboardingRequest) {
        return fillUsersAndOnboarding(onboardingMapper.toEntity(onboardingRequest), onboardingRequest.getUsers());
    }

    @Override
    public Uni<OnboardingResponse> onboardingPa(OnboardingPaRequest onboardingRequest) {
        return fillUsersAndOnboarding(onboardingMapper.toEntity(onboardingRequest), onboardingRequest.getUsers());
    }

    public Uni<OnboardingResponse> fillUsersAndOnboarding(Onboarding onboarding, List<UserRequest> userRequests) {
        onboarding.setExpiringDate(OffsetDateTime.now().plusDays(onboardingExpireDate).toLocalDateTime());
        onboarding.setCreatedAt(LocalDateTime.now());
        onboarding.setWorkflowType(getWorkflowType(onboarding));
        onboarding.setStatus(OnboardingStatus.REQUEST);

        return Panache.withTransaction(() -> Onboarding.persist(onboarding).replaceWith(onboarding)
                .onItem().transformToUni(onboardingPersisted -> checkRoleAndRetrieveUsers(userRequests, onboardingPersisted.id.toHexString())
                    .onItem().invoke(onboardingPersisted::setUsers).replaceWith(onboardingPersisted))
                .onItem().transformToUni(this::checkProductAndReturnOnboarding)
                .onItem().transformToUni(this::addParentDescritpionForAooOrUo)
                .onItem().transformToUni(this::persistAndStartOrchestrationOnboarding)
                .onItem().transform(onboardingMapper::toResponse));
    }

    private Uni<Onboarding> addParentDescritpionForAooOrUo(Onboarding onboarding) {
        if (InstitutionType.PA == onboarding.getInstitution().getInstitutionType()) {
            if (InstitutionPaSubunitType.AOO == onboarding.getInstitution().getSubunitType()) {
                return addParentDescriptionForAOO(onboarding);
            } else if (InstitutionPaSubunitType.UO == onboarding.getInstitution().getSubunitType()) {
                return addParentDescriptionForUO(onboarding);
            }
        }
        return Uni.createFrom().item(onboarding);
    }

    private Uni<Onboarding> addParentDescriptionForUO(Onboarding onboarding) {
        return uoApi.findByUnicodeUsingGET1(onboarding.getInstitution().getSubunitCode(), null)
                .onItem().invoke(uoResource -> onboarding.getInstitution().setParentDescription(uoResource.getDenominazioneEnte()))
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format(UO_NOT_FOUND.getMessage(), onboarding.getInstitution().getSubunitCode())))
                        : Uni.createFrom().failure(ex))
                .replaceWith(onboarding);

    }

    private Uni<Onboarding> addParentDescriptionForAOO(Onboarding onboarding) {
        return aooApi.findByUnicodeUsingGET(onboarding.getInstitution().getSubunitCode(), null)
                .onItem().invoke(aooResource -> onboarding.getInstitution().setParentDescription(aooResource.getDenominazioneEnte()))
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format(AOO_NOT_FOUND.getMessage(), onboarding.getInstitution().getSubunitCode())))
                        : Uni.createFrom().failure(ex))
                .replaceWith(onboarding);
    }

    public Uni<Onboarding> persistAndStartOrchestrationOnboarding(Onboarding onboarding) {
        final List<Onboarding> onboardings = new ArrayList<>();
        onboardings.add(onboarding);

        if (Boolean.TRUE.equals(onboardingOrchestrationEnabled)) {
            return Onboarding.persistOrUpdate(onboardings)
                    .onItem().transformToUni(saved -> orchestrationApi.apiStartOnboardingOrchestrationGet(onboarding.getId().toHexString())
                    .replaceWith(onboarding));
        } else {
            return Onboarding.persistOrUpdate(onboardings)
                    .replaceWith(onboarding);
        }
    }

    /**
     * Identify which workflow must be trigger during onboarding process.
     * Each workflow consist of different activities such as creating contract or sending appropriate mail.
     * For more information look at <a href="https://pagopa.atlassian.net/wiki/spaces/SCP/pages/776339638/DR+-+Domain+Onboarding">...</a>
     *
     * @param onboarding actual onboarding request
     * @return WorkflowType
     */
    private WorkflowType getWorkflowType(Onboarding onboarding) {
        InstitutionType institutionType = onboarding.getInstitution().getInstitutionType();
        if(InstitutionType.PT.equals(institutionType)){
            return WorkflowType.FOR_APPROVE_PT;
        }

        if(InstitutionType.PA.equals(institutionType)
                || isGspAndProdInterop(institutionType, onboarding.getProductId())
                || InstitutionType.SA.equals(institutionType)
                || InstitutionType.AS.equals(institutionType)) {
            return WorkflowType.CONTRACT_REGISTRATION;
        }

        if(InstitutionType.PG.equals(institutionType)) {
            return WorkflowType.CONFIRMATION;
        }

        return WorkflowType.FOR_APPROVE;
    }

    private boolean isGspAndProdInterop(InstitutionType institutionType, String productId) {
        return InstitutionType.GSP == institutionType
                && productId.equals(PROD_INTEROP.getValue());
    }

    private Uni<Product> product(String productId) {
        return Uni.createFrom().item(() -> productService.getProductIsValid(productId))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Onboarding> checkProductAndReturnOnboarding(Onboarding onboarding) {

        return product(onboarding.getProductId())
                .onFailure().transform(ex -> new OnboardingNotAllowedException(String.format(UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED,
                        onboarding.getInstitution().getTaxCode(),
                        onboarding.getProductId()), DEFAULT_ERROR.getCode()))
                .onItem().transformToUni(product -> {

                    /* if PT and product is not delegable, throw an exception */
                    if(InstitutionType.PT == onboarding.getInstitution().getInstitutionType() && !product.isDelegable()) {
                        return Uni.createFrom().failure(new OnboardingNotAllowedException(String.format(ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE,
                                onboarding.getInstitution().getTaxCode(),
                                onboarding.getProductId()), DEFAULT_ERROR.getCode()));
                    }

                    validateProductRole(onboarding.getUsers(), Objects.nonNull(product.getParent())
                                            ? product.getParent().getRoleMappings()
                                            : product.getRoleMappings());

                    return verifyAlreadyOnboardingForProductAndProductParent(onboarding, product);
                });
    }

    private Uni<Onboarding> verifyAlreadyOnboardingForProductAndProductParent(Onboarding onboarding, Product product) {
        String institutionTaxCode = onboarding.getInstitution().getTaxCode();
        String institutionSubunitCode = onboarding.getInstitution().getSubunitCode();

        return (Objects.nonNull(product.getParent())
                //If product has parent, I must verify if onboarding is present for parent and child
                ? checkIfAlreadyOnboardingAndValidateAllowedMap(product.getParentId(), institutionTaxCode, institutionSubunitCode)
                    .onItem().transformToUni( ignore -> checkIfAlreadyOnboardingAndValidateAllowedMap(product.getId(), institutionTaxCode, institutionSubunitCode))
                //If product is a root, I must only verify if onboarding for root
                : checkIfAlreadyOnboardingAndValidateAllowedMap(product.getId(), institutionTaxCode, institutionSubunitCode)
        ).replaceWith(onboarding);
    }

    private Uni<Boolean> checkIfAlreadyOnboardingAndValidateAllowedMap(String productId, String institutionTaxCode, String institutionSubuniCode) {

        if (!onboardingValidationStrategy.validate(productId, institutionTaxCode)) {
            return Uni.createFrom().failure(new OnboardingNotAllowedException(String.format(ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE,
                    institutionTaxCode,
                    productId),
                    DEFAULT_ERROR.getCode()));
        }

        return onboardingApi.verifyOnboardingInfoUsingHEAD(institutionTaxCode, productId, institutionSubuniCode)
                .onItem().failWith(() -> new InvalidRequestException(String.format(UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_ALREADY_ONBOARDED,
                        institutionTaxCode, productId),
                        DEFAULT_ERROR.getCode()))
                .onFailure(ClientWebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException)ex).getResponse().getStatus() == 404
                    ? Uni.createFrom().item(Response.noContent().build())
                    : Uni.createFrom().failure(new RuntimeException(ex.getMessage())))
                .replaceWith(Boolean.TRUE);
    }

    private void validateProductRole(List<User> users, Map<PartyRole, ProductRoleInfo> roleMappings) {
        try {
            if(Objects.isNull(roleMappings) || roleMappings.isEmpty())
                throw new IllegalArgumentException("Role mappings is required");
            users.forEach(userInfo -> {
                if(Objects.isNull(roleMappings.get(userInfo.getRole())))
                    throw new IllegalArgumentException(String.format(ATLEAST_ONE_PRODUCT_ROLE_REQUIRED, userInfo.getRole()));
                if(Objects.isNull((roleMappings.get(userInfo.getRole()).getRoles())))
                    throw new IllegalArgumentException(String.format(ATLEAST_ONE_PRODUCT_ROLE_REQUIRED, userInfo.getRole()));
                if(roleMappings.get(userInfo.getRole()).getRoles().size() != 1)
                    throw new IllegalArgumentException(String.format(MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE, userInfo.getRole()));
                userInfo.setProductRole(roleMappings.get(userInfo.getRole()).getRoles().get(0).getCode());
            });
        } catch (IllegalArgumentException e){
            throw new OnboardingNotAllowedException(e.getMessage(), DEFAULT_ERROR.getCode());
        }
    }

    public Uni<List<User>> checkRoleAndRetrieveUsers(List<UserRequest> users, String onboardingId) {

        List<PartyRole> validRoles = List.of(PartyRole.MANAGER, PartyRole.DELEGATE);

        List<UserRequest> usersNotValidRole =  users.stream()
                .filter(user -> !validRoles.contains(user.getRole()))
                .toList();
        if (!usersNotValidRole.isEmpty()) {
            String usersNotValidRoleString = usersNotValidRole.stream()
                    .map(user -> user.getRole().toString())
                    .collect(Collectors.joining(","));
            return Uni.createFrom().failure( new InvalidRequestException(String.format(CustomError.ROLES_NOT_ADMITTED_ERROR.getMessage(), usersNotValidRoleString),
                    CustomError.ROLES_NOT_ADMITTED_ERROR.getCode()));
        }

        return Multi.createFrom().iterable(users)
                        .onItem().transformToUni(user -> userRegistryApi
                                .searchUsingPOST(USERS_FIELD_LIST, new UserSearchDto().fiscalCode(user.getTaxCode()))

                            /* retrieve userId, if found will eventually update some fields */
                            .onItem().transformToUni(userResource -> createUpdateUserRequest(user, userResource, onboardingId)
                                .map(userUpdateRequest -> userRegistryApi.updateUsingPATCH(userResource.getId().toString(), userUpdateRequest)
                                        .replaceWith(userResource.getId()))
                                .orElse(Uni.createFrom().item(userResource.getId())))
                            /* if not found 404, will create new user */
                            .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException)ex).getResponse().getStatus() == 404
                                ? userRegistryApi.saveUsingPATCH(createSaveUserDto(user, onboardingId)).onItem().transform(UserId::getId)
                                : Uni.createFrom().failure(ex))
                            .onItem().transform(userResourceId -> User.builder()
                                .id(userResourceId.toString())
                                .role(user.getRole())
                                .build())
                )
                .concatenate().collect().asList();
    }

    private SaveUserDto createSaveUserDto(UserRequest model, String onboardingId) {
        SaveUserDto resource = new SaveUserDto();
        resource.setFiscalCode(model.getTaxCode());
        resource.setName(new CertifiableFieldResourceOfstring()
                .value(model.getName())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        resource.setFamilyName(new CertifiableFieldResourceOfstring()
                .value(model.getSurname())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));

        if (Objects.nonNull(onboardingId)) {
            WorkContactResource contact = new WorkContactResource();
            contact.setEmail(new CertifiableFieldResourceOfstring()
                    .value(model.getEmail())
                    .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
            resource.setWorkContacts(Map.of(workContactsKey.apply(onboardingId), contact));
        }
        return resource;
    }

    protected static Optional<MutableUserFieldsDto> createUpdateUserRequest(UserRequest user, UserResource foundUser, String onboardingId) {
        Optional<MutableUserFieldsDto> mutableUserFieldsDto = Optional.empty();
        if (isFieldToUpdate(foundUser.getName(), user.getName())) {
            MutableUserFieldsDto dto = new MutableUserFieldsDto();
            dto.setName(new CertifiableFieldResourceOfstring()
                    .value(user.getName())
                    .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
            mutableUserFieldsDto = Optional.of(dto);
        }
        if (isFieldToUpdate(foundUser.getFamilyName(), user.getSurname())) {
            MutableUserFieldsDto dto = mutableUserFieldsDto.orElseGet(MutableUserFieldsDto::new);
            dto.setFamilyName(new CertifiableFieldResourceOfstring()
                    .value(user.getSurname())
                    .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
            mutableUserFieldsDto = Optional.of(dto);
        }

        if (foundUser.getWorkContacts() == null
                || !foundUser.getWorkContacts().containsKey(onboardingId)
                || isFieldToUpdate(foundUser.getWorkContacts().get(onboardingId).getEmail(), user.getEmail())) {
            MutableUserFieldsDto dto = mutableUserFieldsDto.orElseGet(MutableUserFieldsDto::new);
            final WorkContactResource workContact = new WorkContactResource();
            workContact.setEmail(new CertifiableFieldResourceOfstring()
                    .value(user.getEmail())
                    .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
            dto.setWorkContacts(Map.of(workContactsKey.apply(onboardingId), workContact));
            mutableUserFieldsDto = Optional.of(dto);
        }
        return mutableUserFieldsDto;
    }

    private static boolean isFieldToUpdate(CertifiableFieldResourceOfstring certifiedField, String value) {
        boolean isToUpdate = true;
        if (certifiedField != null) {
            boolean isNoneCertification = CertifiableFieldResourceOfstring.CertificationEnum.NONE.equals(certifiedField.getCertification());
            boolean isSameValue = isNoneCertification ? certifiedField.getValue().equals(value) : certifiedField.getValue().equalsIgnoreCase(value);

            if (isSameValue) {
                isToUpdate = false;
            } else if (!isNoneCertification) {
                throw new UpdateNotAllowedException(String.format("Update user request not allowed because of value %s", value));
            }
        }
        return isToUpdate;
    }

    @Override
    public Uni<OnboardingGet> approve(String onboardingId) {
        return approve(onboardingId, id -> orchestrationApi.apiStartOnboardingOrchestrationGet(id));
    }

    @Override
    public Uni<OnboardingGet> approveCompletion(String onboardingId) {
        return approve(onboardingId, id -> orchestrationApi.apiStartOnboardingCompletionOrchestrationGet(id));
    }


    private Uni<OnboardingGet> approve(String onboardingId, Function<String, Uni<OrchestrationResponse>> approveFunction) {
        return retrieveOnboardingAndCheckIfExpired(onboardingId)
                .onItem().transformToUni(this::checkIfToBeValidated)
                //Fail if onboarding exists for a product
                .onItem().transformToUni(onboarding -> product(onboarding.getProductId())
                        .onItem().transformToUni(product -> verifyAlreadyOnboardingForProductAndProductParent(onboarding, product))
                )
                .onItem().transformToUni(onboarding -> onboardingOrchestrationEnabled
                        ? approveFunction.apply(onboardingId).map(ignore -> onboarding)
                        : Uni.createFrom().item(onboarding))
                .map(onboardingMapper::toGetResponse);
    }

    @Override
    public Uni<Onboarding> complete(String onboardingId, File contract) {

        if (Boolean.TRUE.equals(isVerifyEnabled)) {
            //Retrieve as Tuple: managers fiscal-code from user registry and contract digest
            //At least, verify contract signature using both
            Function<Onboarding, Uni<Onboarding>> verification = onboarding -> Uni.combine().all()
                    .unis(retrieveOnboardingUserFiscalCodeList(onboarding), retrieveContractDigest(onboardingId))
                    .asTuple()
                    .onItem().transform(inputSignatureVerification -> {
                        signatureService.verifySignature(contract,
                                inputSignatureVerification.getItem2(),
                                inputSignatureVerification.getItem1());
                        return onboarding;
                    });

            return complete(onboardingId, contract, verification);
        } else {
            return completeWithoutSignatureVerification(onboardingId, contract);
        }
    }

    @Override
    public Uni<Onboarding> completeWithoutSignatureVerification(String onboardingId, File contract) {
        Function<Onboarding, Uni<Onboarding>> verification = ignored -> Uni.createFrom().item(ignored);
        return complete(onboardingId, contract, verification);
    }

    private Uni<Onboarding> complete(String onboardingId, File contract, Function<Onboarding, Uni<Onboarding>> verificationContractSignature) {

        return retrieveOnboardingAndCheckIfExpired(onboardingId)
                .onItem().transformToUni(verificationContractSignature::apply)
                //Fail if onboarding exists for a product
                .onItem().transformToUni(onboarding -> product(onboarding.getProductId())
                        .onItem().transformToUni(product -> verifyAlreadyOnboardingForProductAndProductParent(onboarding, product))
                )
                //Upload contract on storage
                .onItem().transformToUni(onboarding -> uploadSignedContractAndUpdateToken(onboardingId, contract)
                            .map(ignore -> onboarding))
                // Start async activity if onboardingOrchestrationEnabled is true
                .onItem().transformToUni(onboarding -> onboardingOrchestrationEnabled
                        ? orchestrationApi.apiStartOnboardingCompletionOrchestrationGet(onboarding.getId().toHexString())
                        .map(ignore -> onboarding)
                        : Uni.createFrom().item(onboarding));
    }

    private Uni<String> uploadSignedContractAndUpdateToken(String onboardingId, File contract) {
        return retrieveToken(onboardingId)
            .onItem().transformToUni(token -> Uni.createFrom().item(Unchecked.supplier(() -> {
                    final String path = String.format("%s%s", pathContracts, onboardingId);
                    final String filename = String.format("signed_%s", token.getContractFilename());

                    try {
                        return azureBlobClient.uploadFile(path, filename, Files.readAllBytes(contract.toPath()));
                    } catch (IOException e) {
                        throw new OnboardingNotAllowedException(GENERIC_ERROR.getCode(),
                                "Error on upload contract for onboarding with id " + onboardingId);
                    }
                }))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transformToUni(filepath -> Token.update("contractSigned", filepath)
                                .where("_id", token.getId())
                                .replaceWith(filepath))
            );
    }


    private Uni<Onboarding> retrieveOnboardingAndCheckIfExpired(String onboardingId) {
        //Retrieve Onboarding if exists
        return Onboarding.findByIdOptional(new ObjectId(onboardingId))
                .onItem().transformToUni(opt -> opt
                        //I must cast to Onboarding because findByIdOptional return a generic ReactiveEntity
                        .map(onboarding -> (Onboarding) onboarding)
                        //Check if onboarding is expired
                        .filter(onboarding -> !isOnboardingExpired(onboarding.getExpiringDate()))
                        .map(onboarding -> Uni.createFrom().item(onboarding))
                        .orElse(Uni.createFrom().failure(new InvalidRequestException(String.format(ONBOARDING_EXPIRED.getMessage(),
                                onboardingId, ONBOARDING_EXPIRED.getCode())))));
    }


    private Uni<Onboarding> checkIfToBeValidated(Onboarding onboarding) {
        return OnboardingStatus.TO_BE_VALIDATED.equals(onboarding.getStatus())
                ? Uni.createFrom().item(onboarding)
                : Uni.createFrom().failure(new InvalidRequestException(String.format(ONBOARDING_NOT_TO_BE_VALIDATED.getMessage(),
                    onboarding.getInstitution(), ONBOARDING_NOT_TO_BE_VALIDATED.getCode())));
    }

    public static boolean isOnboardingExpired(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        return Objects.nonNull(dateTime) && (now.isEqual(dateTime) || now.isAfter(dateTime));
    }



    private Uni<String> retrieveContractDigest(String onboardingId) {
        return retrieveToken(onboardingId)
                .map(Token::getChecksum);
    }
    private Uni<Token> retrieveToken(String onboardingId) {
        return Token.list("onboardingId", onboardingId)
                .map(tokens -> tokens.stream().findFirst()
                        .map(token -> (Token) token)
                        .orElseThrow());
    }

    private Uni<List<String>> retrieveOnboardingUserFiscalCodeList(Onboarding onboarding) {
        return Multi.createFrom().iterable(onboarding.getUsers().stream()
                        .filter(user -> PartyRole.MANAGER.equals(user.getRole()))
                        .map(User::getId)
                        .toList())
                .onItem().transformToUni(userId -> userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, userId))
                .merge().collect().asList()
                .onItem().transform(usersResource -> usersResource.stream().map(UserResource::getFiscalCode).toList());
    }

    @Override
    public Uni<OnboardingGetResponse> onboardingGet(String productId, String taxCode, String status, String from, String to, Integer page, Integer size) {
        Document sort = QueryUtils.buildSortDocument(Onboarding.Fields.createdAt.name(), SortEnum.DESC);
        Map<String, String> queryParameter = QueryUtils.createMapForOnboardingQueryParameter(productId, taxCode, status, from, to);
        Document query = QueryUtils.buildQuery(queryParameter);

        return Uni.combine().all().unis(
                        runQuery(query, sort).page(page, size).list(),
                        runQuery(query, null).count()
                ).asTuple()
                .map(this::constructOnboardingGetResponse);
    }

    private ReactivePanacheQuery<Onboarding> runQuery(Document query, Document sort) {
        return Onboarding.find(query, sort);
    }

    private OnboardingGetResponse constructOnboardingGetResponse(Tuple2<List<Onboarding>, Long> tuple) {
        OnboardingGetResponse onboardingGetResponse = new OnboardingGetResponse();
        onboardingGetResponse.setCount(tuple.getItem2());
        onboardingGetResponse.setItems(convertOnboardingListToResponse(tuple.getItem1()));
        return onboardingGetResponse;
    }

    private List<OnboardingGet> convertOnboardingListToResponse(List<Onboarding> item1) {
        return item1.stream()
                .map(onboardingMapper::toGetResponse)
                .toList();
    }

    @Override
    public Uni<Long> deleteOnboarding(String onboardingId) {
        return checkOnboardingIdFormat(onboardingId)
                .onItem()
                .transformToUni(id -> updateStatus(onboardingId, OnboardingStatus.DELETED));
    }

    /**
     * Returns an onboarding record by its ID only if its status is PENDING.
     * This feature is crucial for ensuring that the onboarding process can be completed only when
     * the onboarding status is appropriately set to PENDING.
     * @param onboardingId String
     * @return OnboardingGet
     */
    @Override
    public Uni<OnboardingGet> onboardingPending(String onboardingId) {
        return onboardingGet(onboardingId)
                .flatMap(onboardingGet -> OnboardingStatus.PENDING.name().equals(onboardingGet.getStatus())
                    ? Uni.createFrom().item(onboardingGet)
                    : Uni.createFrom().failure(new ResourceNotFoundException(String.format("Onboarding with id %s not found or not in PENDING status!",onboardingId))));
    }

    @Override
    public Uni<OnboardingGet> onboardingGet(String onboardingId) {
        return Onboarding.findByIdOptional(new ObjectId(onboardingId))
                .onItem().transformToUni(opt -> opt
                        //I must cast to Onboarding because findByIdOptional return a generic ReactiveEntity
                        .map(onboarding -> (Onboarding) onboarding)
                        .map(onboardingMapper::toGetResponse)
                        .map(onboardingGet -> Uni.createFrom().item(onboardingGet))
                        .orElse(Uni.createFrom().failure(new ResourceNotFoundException(String.format("Onboarding with id %s not found!",onboardingId)))));
    }

    @Override
    public Uni<OnboardingGet> onboardingGetWithUserInfo(String onboardingId) {
        return onboardingGet(onboardingId)
                .flatMap(onboardingGet -> fillOnboardingWithUserInfo(onboardingGet.getUsers(), workContactsKey.apply(onboardingId))
                        .replaceWith(onboardingGet));
    }

    private Uni<List<UserResponse>> fillOnboardingWithUserInfo(List<UserResponse> users, String workContractId) {
        return Multi.createFrom().iterable(users)
                .onItem().transformToUni(userResponse -> userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST ,userResponse.getId())
                        .onItem().invoke(userResource -> userMapper.fillUserResponse(userResource, userResponse))
                        .onItem().invoke(userResource -> Optional.ofNullable(userResource.getWorkContacts())
                                .filter(map -> map.containsKey(workContractId))
                                .map(map -> map.get(workContractId))
                                .filter(workContract -> StringUtils.isNotBlank(workContract.getEmail().getValue()))
                                .map(workContract -> workContract.getEmail().getValue())
                                .ifPresent(userResponse::setEmail))
                        .replaceWith(userResponse)
                    )
                .merge().collect().asList();
    }

    private static Uni<Long> updateStatus(String onboardingId, OnboardingStatus onboardingStatus ) {
        return Onboarding.update(Onboarding.Fields.status.name(), onboardingStatus)
                .where("_id", onboardingId)
                .onItem().transformToUni(updateItemCount -> {
                    if (updateItemCount == 0) {
                        return Uni.createFrom().failure(new InvalidRequestException(String.format(ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED, onboardingId)));
                    }
                    return Uni.createFrom().item(updateItemCount);
                });
    }

    private Uni<String> checkOnboardingIdFormat(String onboardingId) {
        if (!ObjectId.isValid(onboardingId)) {
            return Uni.createFrom().failure(new InvalidRequestException(String.format(INVALID_OBJECTID, onboardingId)));
        }
        return Uni.createFrom().item(onboardingId);
    }
}
