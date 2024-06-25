package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.AggregateInstitution;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Objects;

@Mapper(componentModel = "cdi")
public interface OnboardingMapper {

    @Mapping(target = "institution", expression = "java(mapInstitution(input.getAggregate(), input.getInstitution()))")
    @Mapping(target = "aggregator", source = "institution")
    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID().toString())")
    Onboarding mapToOnboarding(OnboardingAggregateOrchestratorInput input);

    @Mapping(target = "aggregate", expression = "java(mapFromAggregateInstitution(aggregateInstitution))")
    OnboardingAggregateOrchestratorInput mapToOnboardingAggregateOrchestratorInput(Onboarding onboarding, AggregateInstitution aggregateInstitution);

    /**
     * We need to create an explicit method to map the aggregate into the institution field of the new onboarding entity
     * because the data related to institutionType and origin must be retrieved from the aggregator,
     * which corresponds to the institution field in the input object.
     */
    @Named("mapInstitution")
    default Institution mapInstitution(Institution aggregate, Institution institution) {
        if (Objects.nonNull(institution)) {
            aggregate.setOrigin(institution.getOrigin());
            aggregate.setInstitutionType(institution.getInstitutionType());
        }
        return aggregate;
    }

    Institution mapFromAggregateInstitution(AggregateInstitution aggregateInstitution);
}
