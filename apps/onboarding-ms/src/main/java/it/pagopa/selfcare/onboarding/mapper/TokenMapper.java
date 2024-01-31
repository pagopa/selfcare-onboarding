package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.controller.response.TokenResponse;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "cdi")
public interface TokenMapper {

    @Mapping(target = "id", expression = "java(objectIdToString(entity.getId()))")
    TokenResponse toResponse(Token entity);


    @Named("objectIdToString")
    default String objectIdToString(ObjectId objectId) {
        return objectId.toHexString();
    }
}
