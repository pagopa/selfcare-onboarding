package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openapi.quarkus.user_json.model.PartyRole;

import java.util.Objects;

@Mapper(componentModel = "cdi")
public interface ProductMapper {

    @Mapping(source = "onboarding.productId", target = "productId")
    @Mapping(source = "user.productRole", target = "productRole")
    @Mapping(target = "role", expression = "java(getProductRole(user))")
    org.openapi.quarkus.user_json.model.Product toProduct(Onboarding onboarding, User user);

    @Named("getProductRole")
    default PartyRole getProductRole(User user) {
        if(Objects.nonNull(user.getRole())) {
            return PartyRole.valueOf(user.getRole().name());
        }
        return null;
    }
}
