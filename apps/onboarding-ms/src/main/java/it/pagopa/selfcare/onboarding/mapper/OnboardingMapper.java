package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Objects;
import java.util.UUID;

@Mapper(componentModel = "cdi", imports = UUID.class)
public interface OnboardingMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    Onboarding toEntity(OnboardingPaRequest model);
    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    Onboarding toEntity(OnboardingPspRequest request);
    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    Onboarding toEntity(OnboardingDefaultRequest request);
    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    Onboarding toEntity(OnboardingSaRequest request);
    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "activatedAt", source = "contractImported.createdAt")
    Onboarding toEntity(OnboardingImportRequest request);

    @Mapping(source = "taxCode", target = "institution.taxCode")
    @Mapping(source = "businessName", target = "institution.description")
    @Mapping(source = "digitalAddress", target = "institution.digitalAddress")
    @Mapping(source = "origin", target = "institution.origin")
    @Mapping(source = "institutionType", target = "institution.institutionType")
    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    Onboarding toEntity(OnboardingPgRequest request);

    OnboardingResponse toResponse(Onboarding model);

    OnboardingGet toGetResponse(Onboarding model);

    @Named("toUpperCase")
    default String toUpperCase(String recipientCode) {
        return Objects.nonNull(recipientCode) ? recipientCode.toUpperCase() : null;
    }

}
