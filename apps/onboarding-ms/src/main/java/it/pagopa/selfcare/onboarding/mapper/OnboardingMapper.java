package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "cdi")
public interface OnboardingMapper {

    Onboarding toEntity(OnboardingPaRequest request);
    Onboarding toEntity(OnboardingPspRequest request);
    Onboarding toEntity(OnboardingDefaultRequest request);
    Onboarding toEntity(OnboardingSaRequest request);

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
}
