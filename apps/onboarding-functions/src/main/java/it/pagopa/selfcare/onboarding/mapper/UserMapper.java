package it.pagopa.selfcare.onboarding.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapi.quarkus.core_json.model.Person;
import org.openapi.quarkus.user_registry_json.model.UserResource;

@Mapper(componentModel = "cdi")
public interface UserMapper {

    @Mapping(source = "email.value", target = "email")
    @Mapping(source = "name.value", target = "name")
    @Mapping(source = "fiscalCode", target = "taxCode")
    @Mapping(source = "familyName.value", target = "surname")
    Person toPerson(UserResource userResource);

}
