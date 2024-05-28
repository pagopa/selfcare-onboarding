package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.entity.NotificationToSend;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface NotificationMapper {

    NotificationToSend toSCContractsFD(Onboarding onboarding);
    NotificationToSend toSCContractsSAP(Onboarding onboarding);
    NotificationToSend toSCContracts(Onboarding onboarding);
}
