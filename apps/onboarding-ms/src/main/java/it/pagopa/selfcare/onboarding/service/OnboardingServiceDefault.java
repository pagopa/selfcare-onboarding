package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mongodb.panache.common.reactive.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.unchecked.Unchecked;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.strategy.OnboardingValidationStrategy;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.openapi.quarkus.core_json.api.OnboardingApi;
import org.openapi.quarkus.onboarding_functions_json.api.OrchestrationApi;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_INTEROP;
import static it.pagopa.selfcare.onboarding.constants.CustomError.DEFAULT_ERROR;
import static it.pagopa.selfcare.onboarding.util.GenericError.GENERIC_ERROR;
import static it.pagopa.selfcare.onboarding.util.GenericError.ONBOARDING_EXPIRED;

@ApplicationScoped
public class OnboardingServiceDefault implements OnboardingService {
    protected static final String ATLEAST_ONE_PRODUCT_ROLE_REQUIRED = "At least one Product role related to %s Party role is required";
    protected static final String MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE = "More than one Product role related to %s Party role is available. Cannot automatically set the Product role";
    private static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE = "Institution with external id '%s' is not allowed to onboard '%s' product";
    private static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE = "Institution with external id '%s' is not allowed to onboard '%s' product because it is not delegable";
    public static final String UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED = "Unable to complete the onboarding for institution with taxCode '%s' to product '%s', the product is dismissed.";

    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
    public static final String USERS_FIELD_TAXCODE = "fiscalCode";
    public static final String UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_ALREADY_ONBOARDED = "Unable to complete the onboarding for institution with taxCode '%s' to product '%s'.";
    @RestClient
    @Inject
    UserApi userRegistryApi;

    @RestClient
    @Inject
    OnboardingApi onboardingApi;

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
        onboarding.setExpiringDate( OffsetDateTime.now().plus(onboardingExpireDate, ChronoUnit.DAYS).toLocalDateTime());
        onboarding.setCreatedAt(LocalDateTime.now());
        onboarding.setWorkflowType(getWorkflowType(onboarding));

        return Panache.withTransaction(() -> Onboarding.persist(onboarding).replaceWith(onboarding)
                .onItem().transformToUni(onboardingPersisted -> checkRoleAndRetrieveUsers(userRequests, onboardingPersisted.id.toHexString())
                    .onItem().invoke(onboardingPersisted::setUsers).replaceWith(onboardingPersisted))
                .onItem().transformToUni(this::checkProductAndReturnOnboarding)
                .onItem().transformToUni(this::persistAndStartOrchestrationOnboarding)
                .onItem().transform(onboardingMapper::toResponse));
    }

    public Uni<Onboarding> persistAndStartOrchestrationOnboarding(Onboarding onboarding) {
        final List<Onboarding> onboardings = new ArrayList<>();
        onboardings.add(onboarding);

        if(onboardingOrchestrationEnabled) {
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
     * For more information look at https://pagopa.atlassian.net/wiki/spaces/SCP/pages/776339638/DR+-+Domain+Onboarding
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

    public Uni<Onboarding> checkProductAndReturnOnboarding(Onboarding onboarding) {

        final Product product;

        try {
            product = Optional.ofNullable(productService.getProductIsValid(onboarding.getProductId()))
                    .orElseThrow(() -> new OnboardingNotAllowedException(String.format(UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED,
                        onboarding.getInstitution().getTaxCode(),
                        onboarding.getProductId()), DEFAULT_ERROR.getCode()));
        } catch (ProductNotFoundException | IllegalArgumentException e){
            throw new OnboardingNotAllowedException(String.format(UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED,
                    onboarding.getInstitution().getTaxCode(),
                    onboarding.getProductId()), DEFAULT_ERROR.getCode());
        }

        /* if PT and product is not delegable, throw an exception */
        if(InstitutionType.PT == onboarding.getInstitution().getInstitutionType() && !product.isDelegable()) {
                throw new OnboardingNotAllowedException(String.format(ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE,
                onboarding.getInstitution().getTaxCode(),
                onboarding.getProductId()), DEFAULT_ERROR.getCode());
        }

        validateProductRole(onboarding.getUsers(), Objects.nonNull(product.getParent())
                ? product.getParent().getRoleMappings()
                : product.getRoleMappings());

        return verifyAlreadyOnboardingForProductAndProductParent(onboarding, product);
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
            resource.setWorkContacts(Map.of(createWorkContractId(onboardingId), contact));
        }
        return resource;
    }

    private static String createWorkContractId(String onboardingId) {
        return String.format("obg_%s", onboardingId);
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
            dto.setWorkContacts(Map.of(createWorkContractId(onboardingId), workContact));
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
    public Uni<Onboarding> complete(String onboardingId, File contract) {

        if(isVerifyEnabled) {
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
                .onItem().transformToUni(onboarding -> {
                    Product product = productService.getProductIsValid(onboarding.getProductId());
                    return verifyAlreadyOnboardingForProductAndProductParent(onboarding, product);
                })
                //Upload contract on storage
                .onItem().transformToUni(onboarding -> uploadSignedContract(onboardingId, contract)
                        .onItem().transform(ignore -> onboarding))
                // Start async activity
                ;
    }

    private Uni<String> uploadSignedContract(String onboardingId, File contract) {
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
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()));
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

}
