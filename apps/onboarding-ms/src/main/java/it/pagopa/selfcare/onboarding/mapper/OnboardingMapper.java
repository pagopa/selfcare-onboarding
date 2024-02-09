package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Objects;

@Mapper(componentModel = "cdi")
public interface OnboardingMapper {

    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    Onboarding toEntity(OnboardingPaRequest request);
    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    Onboarding toEntity(OnboardingPspRequest request);
    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    Onboarding toEntity(OnboardingDefaultRequest request);
    @Mapping(target = "billing.recipientCode", source = "billing.recipientCode", qualifiedByName = "toUpperCase")
    Onboarding toEntity(OnboardingSaRequest request);
    Onboarding toEntity(OnboardingImportRequest request);

    @Mapping(source = "taxCode", target = "institution.taxCode")
    @Mapping(source = "businessName", target = "institution.description")
    @Mapping(source = "digitalAddress", target = "institution.digitalAddress")
    @Mapping(source = "origin", target = "institution.origin")
    @Mapping(source = "institutionType", target = "institution.institutionType")
    Onboarding toEntity(OnboardingPgRequest request);

    @Mapping(target = "id", expression = "java(objectIdToString(model.getId()))")
    OnboardingResponse toResponse(Onboarding model);

    @Mapping(target = "id", expression = "java(objectIdToString(model.getId()))")
    OnboardingGet toGetResponse(Onboarding model);

    @Named("objectIdToString")
    default String objectIdToString(ObjectId objectId) {
        return objectId.toHexString();
    }

    @Named("toUpperCase")
    default String toUpperCase(String recipientCode) {
        return Objects.nonNull(recipientCode) ? recipientCode.toUpperCase() : null;
    }

}
