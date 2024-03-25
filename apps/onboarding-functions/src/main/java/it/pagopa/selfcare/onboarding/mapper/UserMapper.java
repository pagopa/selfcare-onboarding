package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapi.quarkus.core_json.model.Person;
import org.openapi.quarkus.user_json.model.AddUserRoleDto;
import org.openapi.quarkus.user_registry_json.model.UserResource;

@Mapper(componentModel = "cdi", uses = ProductMapper.class)
public interface UserMapper {

    @Mapping(source = "email.value", target = "email")
    @Mapping(source = "name.value", target = "name")
    @Mapping(source = "fiscalCode", target = "taxCode")
    @Mapping(source = "familyName.value", target = "surname")
    Person toPerson(UserResource userResource);

    @Mapping(source = "institution.id", target = "institutionId")
    @Mapping(source = "institution.description", target = "institutionDescription")
    @Mapping(source = "institution.parentDescription", target = "institutionRootName")
    AddUserRoleDto toUserRole(Onboarding onboarding);




}
