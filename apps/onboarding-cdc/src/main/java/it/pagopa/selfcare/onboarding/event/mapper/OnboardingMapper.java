package it.pagopa.selfcare.onboarding.event.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openapi.quarkus.onboarding_functions_json.model.Onboarding;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Mapper(componentModel = "cdi", imports = UUID.class)
public interface OnboardingMapper {
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(target = "activatedAt", source = "activatedAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(target = "expiringDate", source = "expiringDate", qualifiedByName = "toOffsetDateTime")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "toOffsetDateTime")
    Onboarding toEntity(it.pagopa.selfcare.onboarding.event.entity.Onboarding model);
    @Named("toOffsetDateTime")
    default OffsetDateTime map(String value) {
        LocalDateTime localDateTime = LocalDateTime.parse(value);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return zonedDateTime.toOffsetDateTime();
    }
}
