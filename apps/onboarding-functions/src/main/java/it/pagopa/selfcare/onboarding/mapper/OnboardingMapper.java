package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.Aggregator;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Objects;

@Mapper(componentModel = "cdi")
public interface OnboardingMapper {

    @Mapping(target = "institution", expression = "java(mapInstitution(input.getAggregate(), input.getInstitution()))")
    @Mapping(target = "aggregator", expression = "java(mapAggregator(input.getInstitution()))")
    Onboarding mapToOnboarding(OnboardingAggregateOrchestratorInput input);

    @Named("mapInstitution")
    default Institution mapInstitution(Institution aggregator, Institution institution) {
        if (Objects.nonNull(institution)) {
            aggregator.setOrigin(institution.getOrigin());
            aggregator.setInstitutionType(institution.getInstitutionType());
        }
        return aggregator;
    }

    @Named("mapAggregator")
    default Aggregator mapAggregator(Institution institution) {
        Aggregator aggregator = new Aggregator();
        if (Objects.nonNull(institution)) {
            aggregator.setDescription(institution.getDescription());
            aggregator.setTaxCode(institution.getTaxCode());
            aggregator.setId(institution.getId());
        }
        return aggregator;
    }
}
