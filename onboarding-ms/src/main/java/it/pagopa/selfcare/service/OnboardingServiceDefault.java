package it.pagopa.selfcare.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.client.ProductApi;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.commons.base.utils.InstitutionType;
import it.pagopa.selfcare.constants.CustomError;
import it.pagopa.selfcare.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.controller.request.UserRequest;
import it.pagopa.selfcare.controller.response.OnboardingResponse;
import it.pagopa.selfcare.entity.Onboarding;
import it.pagopa.selfcare.entity.User;
import it.pagopa.selfcare.exception.InvalidRequestException;
import it.pagopa.selfcare.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.mapper.OnboardingMapper;
import it.pagopa.selfcare.repository.OnboardingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import  it.pagopa.selfcare.client.model.ProductRoleInfoOperations;
import it.pagopa.selfcare.client.model.ProductRoleInfoRes;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.*;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.constants.CustomError.DEFAULT_ERROR;

@ApplicationScoped
public class OnboardingServiceDefault implements OnboardingService {
    protected static final String PRODUCT_NOT_FOUND = "Product %s not found!";
    protected static final String ATLEAST_ONE_PRODUCT_ROLE_REQUIRED = "At least one Product role related to %s Party role is required";
    protected static final String MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE = "More than one Product role related to %s Party role is available. Cannot automatically set the Product role";
    private static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE = "Institution with external id '%s' is not allowed to onboard '%s' product because it is not delegable";
    public static final String UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED = "Unable to complete the onboarding for institution with taxCode '%s' to product '%s', the product is dismissed.";

    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
    @RestClient
    @Inject
    UserApi userRegistryApi;

    @RestClient
    @Inject
    ProductApi productApi;

    @Inject
    OnboardingRepository onboardingRepository;

    @Inject
    OnboardingMapper onboardingMapper;

    @Override
    public Uni<OnboardingResponse> onboarding(OnboardingDefaultRequest onboardingRequest) {
        return fillUsersAndOnboarding(onboardingMapper.toEntity(onboardingRequest), onboardingRequest.getUsers());
    }

    @Override
    public Uni<OnboardingResponse> onboardingPsp(OnboardingPspRequest onboardingRequest) {
        return fillUsersAndOnboarding(onboardingMapper.toEntity(onboardingRequest), onboardingRequest.getUsers());
    }

    @Override
    public Uni<OnboardingResponse> onboardingPa(OnboardingPaRequest onboardingRequest) {
        return fillUsersAndOnboarding(onboardingMapper.toEntity(onboardingRequest), onboardingRequest.getUsers());
    }

    public Uni<OnboardingResponse> fillUsersAndOnboarding(Onboarding onboarding, List<UserRequest> userRequests) {

        return checkRoleAndRetrieveUsers(userRequests, List.of(PartyRole.MANAGER, PartyRole.DELEGATE))
                .onItem().invoke(onboarding::setUsers).replaceWith(onboarding)
                .onItem().transformToUni(this::checkProductAndReturnOnboarding)
                .onItem().transformToUni(onboardingRepository::persistOrUpdate)
                .onItem().transform(onboardingMapper::toResponse);
    }

    public Uni<Onboarding> checkProductAndReturnOnboarding(Onboarding onboarding) {

        /* Verify already onboarding for product and product parent */

        return productApi.getProductIsValidUsingGET(onboarding.getProductId())
                .onFailure(ClientWebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException)ex).getResponse().getStatus() == 404
                    ? Uni.createFrom().failure(new InvalidRequestException(String.format(PRODUCT_NOT_FOUND, onboarding.getProductId()), DEFAULT_ERROR.getCode()))
                    : Uni.createFrom().failure(new RuntimeException(ex.getMessage())))
                /* if product is not valid, throw an exception */
                .onItem().ifNull().failWith(new OnboardingNotAllowedException(String.format(UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED,
                        onboarding.getInstitution().getTaxCode(),
                        onboarding.getProductId()), DEFAULT_ERROR.getCode()))
                /* if PT and product is not delegable, throw an exception */
                .onItem().transformToUni(productResource -> InstitutionType.PT == onboarding.getInstitution().getInstitutionType() && !productResource.getDelegable()
                    ? Uni.createFrom().failure(new OnboardingNotAllowedException(String.format(ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE,
                            onboarding.getInstitution().getTaxCode(),
                            onboarding.getProductId()), DEFAULT_ERROR.getCode()))
                    : Uni.createFrom().item(productResource))
                .onItem().invoke(product -> {
                    if(Objects.nonNull(product.getProductOperations()))
                        validateProductRole(onboarding.getUsers(), product.getProductOperations().getRoleMappings());
                    else validateProductRoleRes(onboarding.getUsers(), product.getRoleMappings());
                })
                .replaceWith(onboarding);
    }

    private void validateProductRole(List<User> users, Map<String, ProductRoleInfoOperations> roleMappings) {
        try {
            Assert.notNull(roleMappings, "Role mappings is required");
            users.forEach(userInfo -> {
                Assert.notNull(roleMappings.get(userInfo.getRole().name()),
                        String.format(ATLEAST_ONE_PRODUCT_ROLE_REQUIRED, userInfo.getRole()));
                Assert.notEmpty(roleMappings.get(userInfo.getRole().name()).getRoles(),
                        String.format(ATLEAST_ONE_PRODUCT_ROLE_REQUIRED, userInfo.getRole()));
                Assert.state(roleMappings.get(userInfo.getRole().name()).getRoles().size() == 1,
                        String.format(MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE, userInfo.getRole()));
                userInfo.setProductRole(roleMappings.get(userInfo.getRole().name()).getRoles().get(0).getCode());
            });
        } catch (IllegalArgumentException e){
            throw new OnboardingNotAllowedException(e.getMessage(), DEFAULT_ERROR.getCode());
        }
    }

    private void validateProductRoleRes(List<User> users, Map<String, ProductRoleInfoRes> roleMappings) {
        try {
            Assert.notNull(roleMappings, "Role mappings is required");
            users.forEach(userInfo -> {
                Assert.notNull(roleMappings.get(userInfo.getRole().name()),
                        String.format(ATLEAST_ONE_PRODUCT_ROLE_REQUIRED, userInfo.getRole()));
                Assert.notEmpty(roleMappings.get(userInfo.getRole().name()).getRoles(),
                        String.format(ATLEAST_ONE_PRODUCT_ROLE_REQUIRED, userInfo.getRole()));
                Assert.state(roleMappings.get(userInfo.getRole().name()).getRoles().size() == 1,
                        String.format(MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE, userInfo.getRole()));
                userInfo.setProductRole(roleMappings.get(userInfo.getRole().name()).getRoles().get(0).getCode());
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
