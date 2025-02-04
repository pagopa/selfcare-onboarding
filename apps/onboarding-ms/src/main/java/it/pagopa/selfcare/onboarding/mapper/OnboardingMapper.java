package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.*;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.model.*;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openapi.quarkus.onboarding_functions_json.model.PartyRole;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "cdi", imports = { UUID.class, WorkflowType.class, OnboardingStatus.class })
public interface OnboardingMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    Onboarding toEntity(OnboardingPaRequest model);
    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    Onboarding toEntity(OnboardingPspRequest request);
    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    @Mapping(target = "institution.gpuData", source = "gpuData")
    @Mapping(target = "institution", source = "institution")
    Onboarding toEntity(OnboardingDefaultRequest request);

    @Mapping(target = "users", source = "users", qualifiedByName = "toUsersList")
    OnboardingResponseV1 toOnboardingResponseV1(OnboardingResponse request);

    @Named("toUsersList")
    default List<UserOnboardingResponseV1> toUsersList(List<UserOnboardingResponse> list) {
        if(Objects.isNull(list) || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(this::toUserOnboardingResponseV1)
                .toList();
    }


    @Mapping(target = "role", source = "role", qualifiedByName = "toPartyRoleV1")
    UserOnboardingResponseV1 toUserOnboardingResponseV1(UserOnboardingResponse userOnboardingResponse);

    @Named("toPartyRoleV1")
    default PartyRoleV1 toPartyRoleV1(PartyRole partyRole) {
        return switch (partyRole) {
            case MANAGER -> PartyRoleV1.MANAGER;
            case DELEGATE -> PartyRoleV1.DELEGATE;
            case SUB_DELEGATE -> PartyRoleV1.SUB_DELEGATE;
            case OPERATOR -> PartyRoleV1.OPERATOR;
            case ADMIN_EA -> PartyRoleV1.ADMIN_EA;
            default -> null;
        };
    }

    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    @Mapping(target = "status", expression = "java((newStatus != null) ? newStatus : null)")
    Onboarding toEntity(OnboardingDefaultRequest request, @Context OnboardingStatus newStatus);
    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    Onboarding toEntity(OnboardingSaRequest request);
    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "activatedAt", source = "contractImported.createdAt")
    Onboarding toEntity(OnboardingImportRequest request);
    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "activatedAt", source = "contractImported", qualifiedByName = "getActivatedAt")
    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    Onboarding toEntity(OnboardingImportPspRequest request);
    @Mapping(source = "taxCode", target = "institution.taxCode")
    @Mapping(source = "businessName", target = "institution.description")
    @Mapping(source = "digitalAddress", target = "institution.digitalAddress")
    @Mapping(source = "origin", target = "institution.origin")
    @Mapping(source = "institutionType", target = "institution.institutionType")
    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    Onboarding toEntity(OnboardingPgRequest request);

    @Mapping(source = "taxCode", target = "institution.taxCode")
    @Mapping(source = "origin", target = "institution.origin")
    @Mapping(source = "institutionType", target = "institution.institutionType")
    @Mapping(target = "workflowType", expression = "java(WorkflowType.USERS_PG)")
    @Mapping(target = "status", expression = "java(OnboardingStatus.PENDING)")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    Onboarding toEntity(OnboardingUserPgRequest request);

    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "userRequestUid", source = "userId")
    @Mapping(target = "productId", source = "request.productId")
    @Mapping(target = "workflowType", source = "workflowType")
    @Mapping(target = "status", expression = "java(OnboardingStatus.REQUEST)")
    Onboarding toEntity(OnboardingUserRequest request, String userId, WorkflowType workflowType);

    OnboardingResponse toResponse(Onboarding model);

    OnboardingGet toGetResponse(Onboarding model);

    @Named("toUpperCase")
    default String toUpperCase(String recipientCode) {
        return Objects.nonNull(recipientCode) ? recipientCode.toUpperCase() : null;
    }

    @Named("getActivatedAt")
    default LocalDateTime getActivatedAt(OnboardingImportContract onboardingImportContract) {
        if (Objects.nonNull(onboardingImportContract.getActivatedAt())) {
            return onboardingImportContract.getActivatedAt();
        }
        return onboardingImportContract.getCreatedAt();
    }

    @Named("toWorkflowType")
    default org.openapi.quarkus.onboarding_functions_json.model.WorkflowType toWorkflowType(WorkflowType workflowType) {
        if (Objects.isNull(workflowType)) {
            return null;
        }

        return org.openapi.quarkus.onboarding_functions_json.model.WorkflowType.valueOf(workflowType.name());
    }

    @Named("toUsers")
    default List<org.openapi.quarkus.onboarding_functions_json.model.User> toUsers(List<User> users) {
        if(Objects.isNull(users) || users.isEmpty()) {
            return null;
        }

        return users.stream()
                .map(user -> new org.openapi.quarkus.onboarding_functions_json.model.User()
                        .id(user.getId())
                        .productRole(user.getProductRole())
                        .userMailUuid(user.getUserMailUuid())
                        .role(toPartyRole(user.getRole())))
                .toList();
    }

    default PartyRole toPartyRole(it.pagopa.selfcare.onboarding.common.PartyRole role) {
        if(Objects.isNull(role)) {
            return null;
        }

        return PartyRole.valueOf(role.name());
    }

    @Named("toOffsetDateTime")
    default OffsetDateTime toOffsetDateTime(java.time.LocalDateTime localDateTime) {
        if (Objects.isNull(localDateTime)) {
            return null;
        }

        return localDateTime.atOffset(java.time.ZoneOffset.UTC);
    }

    Aggregate csvToAggregateAppIo(CsvAggregateAppIo csvAggregateAppIo);

    Aggregate csvToAggregatePagoPa(CsvAggregatePagoPa csvAggregatePagoPa);

    @Mapping(target = "users", source = ".")
    Aggregate csvToAggregateSend(CsvAggregateSend csvAggregateSend);

    default List<Aggregate> mapCsvSendAggregatesToAggregates(List<CsvAggregateSend> csvAggregateSendList) {
        if (csvAggregateSendList == null) {
            return null;
        }
        return csvAggregateSendList.stream()
                .map(this::csvToAggregateSend)
                .collect(Collectors.toList());
    }

    default List<Aggregate> mapCsvAppIoAggregatesToAggregates(List<CsvAggregateAppIo> csvAggregateAppIoList) {
        if (csvAggregateAppIoList == null) {
            return Collections.emptyList();
        }
        return csvAggregateAppIoList.stream()
                .map(this::csvToAggregateAppIo)
                .collect(Collectors.toList());
    }

    default List<AggregateUser> mapUsers(CsvAggregateSend csvAggregateSend) {
        if (csvAggregateSend == null) {
            return Collections.emptyList();
        }
        AggregateUser user = new AggregateUser();
        user.setName(csvAggregateSend.getAdminAggregateName());
        user.setSurname(csvAggregateSend.getAdminAggregateSurname());
        user.setTaxCode(csvAggregateSend.getAdminAggregateTaxCode());
        user.setEmail(csvAggregateSend.getAdminAggregateEmail());
        user.setRole(PartyRole.DELEGATE.value());

        return Collections.singletonList(user);
    }
}
