package it.pagopa.selfcare.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.constants.CustomError;
import it.pagopa.selfcare.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.controller.request.UserRequest;
import it.pagopa.selfcare.controller.response.OnboardingResponse;
import it.pagopa.selfcare.entity.Onboarding;
import it.pagopa.selfcare.entity.User;
import it.pagopa.selfcare.exception.InvalidRequestException;
import it.pagopa.selfcare.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.mapper.OnboardingMapper;
import it.pagopa.selfcare.repository.OnboardingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.*;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class OnboardingServiceDefault implements OnboardingService {

    public static final String USERS_FIELD_LIST = "fiscalCode,name,workContacts";
    @RestClient
    @Inject
    UserApi userRegistryApi;

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

        /** Check if Product is Valid and retrieve */
        /* PT is delegable ?
        if(InstitutionType.PT == onboardingData.getInstitutionType() && !delegable) {
            throw new OnboardingNotAllowedException(String.format(ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE,
                onboardingData.getTaxCode(),
                onboardingData.getProductId()));
        }*/

        /* Check validation on onboarding maps */
        /* Check if role user is valid using Product,
        /* Verify already onboarding for product and product parent */

        return checkRoleAndRetrieveUsers(userRequests, List.of(PartyRole.MANAGER, PartyRole.DELEGATE))
                .onItem().invoke(onboarding::setUsers).replaceWith(onboarding)
                .onItem().transformToUni(onboardingRepository::persistOrUpdate)
                .onItem().transform(onboardingMapper::toResponse);
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
