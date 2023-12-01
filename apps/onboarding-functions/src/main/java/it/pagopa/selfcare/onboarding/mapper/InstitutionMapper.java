package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.entity.Institution;
import org.mapstruct.Mapper;
import org.openapi.quarkus.core_json.model.InstitutionRequest;

@Mapper(componentModel = "cdi")
public interface InstitutionMapper {

    InstitutionRequest toInstitutionRequest(Institution institution);
}
