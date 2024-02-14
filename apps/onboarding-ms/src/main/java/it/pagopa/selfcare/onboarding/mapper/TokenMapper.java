package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.controller.response.TokenResponse;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface TokenMapper {

    TokenResponse toResponse(Token entity);
}
