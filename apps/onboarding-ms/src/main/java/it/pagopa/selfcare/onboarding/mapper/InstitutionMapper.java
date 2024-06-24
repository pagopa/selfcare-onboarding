package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.entity.Institution;
import org.mapstruct.Mapper;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

@Mapper(componentModel = "cdi")
public interface InstitutionMapper {

    Institution toEntity(InstitutionResponse model);

}
