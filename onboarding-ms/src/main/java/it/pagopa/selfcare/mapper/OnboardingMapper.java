package it.pagopa.selfcare.mapper;

import it.pagopa.selfcare.controller.request.*;
import it.pagopa.selfcare.controller.response.OnboardingResponse;
import it.pagopa.selfcare.entity.Onboarding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface OnboardingMapper {

    Onboarding toEntity(OnboardingPaRequest request);
    Onboarding toEntity(OnboardingPspRequest request);
    Onboarding toEntity(OnboardingDefaultRequest request);
    Onboarding toEntity(OnboardingSaRequest request);

    //@Mapping(source = "taxCode", target = "institution.taxCode")
    //@Mapping(source = "businessName", target = "institution.description")
    //@Mapping(source = "digitalAddress", target = "institution.digitalAddress")
    //Onboarding toEntity(OnboardingPgRequest request);

    OnboardingResponse toResponse(Onboarding onboarding);
}
