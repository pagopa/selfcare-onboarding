package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.entity.NotificationToSend;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface NotificationMapper {

    @Mapping(source = "productId", target = "product")
    @Mapping(source = "institution.id", target = "internalIstitutionID")
    NotificationToSend toSCContractsFD(Onboarding onboarding);
    @Mapping(source = "productId", target = "product")
    @Mapping(source = "institution.id", target = "internalIstitutionID")
    NotificationToSend toSCContractsSAP(Onboarding onboarding);
    @Mapping(source = "productId", target = "product")
    @Mapping(source = "institution.id", target = "internalIstitutionID")
    NotificationToSend toSCContracts(Onboarding onboarding);
}
