package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mongodb.panache.common.reactive.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.openapi.quarkus.core_json.api.OnboardingApi;
import org.openapi.quarkus.onboarding_functions_json.api.OrchestrationApi;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.constants.CustomError.DEFAULT_ERROR;

@ApplicationScoped
public class OnboardingServiceDefault implements OnboardingService {
    protected static final String ATLEAST_ONE_PRODUCT_ROLE_REQUIRED = "At least one Product role related to %s Party role is required";
    protected static final String MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE = "More than one Product role related to %s Party role is available. Cannot automatically set the Product role";
    private static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE = "Institution with external id '%s' is not allowed to onboard '%s' product";
    private static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE = "Institution with external id '%s' is not allowed to onboard '%s' product because it is not delegable";
    public static final String UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED = "Unable to complete the onboarding for institution with taxCode '%s' to product '%s', the product is dismissed.";

    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
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

    @ConfigProperty(name = "onboarding.expiring-date")
    Integer onboardingExpireDate;
    @ConfigProperty(name = "onboarding.orchestration.enabled")
    Boolean onboardingOrchestrationEnabled;

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
        return checkRoleAndRetrieveUsers(userRequests, List.of(PartyRole.MANAGER, PartyRole.DELEGATE))
                .onItem().invoke(onboarding::setUsers).replaceWith(onboarding)
                .onItem().transformToUni(this::checkProductAndReturnOnboarding)
                .onItem().transformToUni(this::persistAndStartOrchestrationOnboarding)
                .onItem().transform(onboardingMapper::toResponse);
    }

    public Uni<Onboarding> persistAndStartOrchestrationOnboarding(Onboarding onboarding) {
        final List<Onboarding> onboardings = new ArrayList<>();
        onboardings.add(onboarding);

        if(onboardingOrchestrationEnabled) {
            return Panache.withTransaction(() -> Onboarding.persistOrUpdate(onboardings)
                    .onItem().transformToUni(saved -> orchestrationApi.apiStartOnboardingOrchestrationGet(onboarding.getId().toHexString())
                            .replaceWith(onboarding)));
        } else {
            return Onboarding.persistOrUpdate(onboardings)
                    .replaceWith(onboarding);
        }
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

        /* Verify already onboarding for product and product parent */
        return (Objects.nonNull(product.getParent())
                        ? checkIfAlreadyOnboardingAndValidateAllowedMap(product.getParentId(), onboarding.getInstitution().getTaxCode(), onboarding.getInstitution().getSubunitCode())
                                .onItem().transformToUni( ignore -> checkIfAlreadyOnboardingAndValidateAllowedMap(product.getId(), onboarding.getInstitution().getTaxCode(), onboarding.getInstitution().getSubunitCode()))
                        : checkIfAlreadyOnboardingAndValidateAllowedMap(product.getId(), onboarding.getInstitution().getTaxCode(), onboarding.getInstitution().getSubunitCode())
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

    public Uni<List<User>> checkRoleAndRetrieveUsers(List<UserRequest> users, List<PartyRole> validRoles) {

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
                            .onItem().transformToUni(userResource -> createUpdateUserRequest(user, userResource)
                                .map(userUpdateRequest -> userRegistryApi.updateUsingPATCH(userResource.getId().toString(), userUpdateRequest)
                                        .replaceWith(userResource.getId()))
                                .orElse(Uni.createFrom().item(userResource.getId())))
                            /* if not found 404, will create new user */
                            .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException)ex).getResponse().getStatus() == 404
                                ? userRegistryApi.saveUsingPATCH(createSaveUserDto(user)).onItem().transform(UserId::getId)
                                : Uni.createFrom().failure(ex))
                            .onItem().transform(userResourceId -> User.builder()
                                .id(userResourceId.toString())
                                .role(user.getRole())
                                .build())
                )
                .concatenate().collect().asList();
    }

    private SaveUserDto createSaveUserDto(UserRequest model) {
        SaveUserDto resource = new SaveUserDto();
        resource.setFiscalCode(model.getTaxCode());
        resource.setName(new CertifiableFieldResourceOfstring()
                .value(model.getName())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        resource.setFamilyName(new CertifiableFieldResourceOfstring()
                .value(model.getSurname())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        return resource;
    }

    protected static Optional<MutableUserFieldsDto> createUpdateUserRequest(UserRequest user, UserResource foundUser) {
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
        return mutableUserFieldsDto;
    }

    private static boolean isFieldToUpdate(CertifiableFieldResourceOfstring certifiedField, String value) {
        boolean isToUpdate = true;
        if (certifiedField != null) {
            if (CertifiableFieldResourceOfstring.CertificationEnum.NONE.equals(certifiedField.getCertification())) {
                if (certifiedField.getValue().equals(value)) {
                    isToUpdate = false;
                }
            } else {
                if (certifiedField.getValue().equalsIgnoreCase(value)) {
                    isToUpdate = false;
                } else {
                    throw new UpdateNotAllowedException(String.format("Update user request not allowed because of value %s", value));
                }
            }
        }
        return isToUpdate;
    }

}
