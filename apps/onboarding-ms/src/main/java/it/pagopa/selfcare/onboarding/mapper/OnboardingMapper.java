package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingSaRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.mapstruct.Mapper;

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
