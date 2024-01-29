package it.pagopa.selfcare.onboarding.service.util;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;

import java.util.Objects;

public class OnboardingUtils {

    private static final String ADDITIONAL_INFORMATIONS_REQUIRED = "Additional Informations is required when institutionType is GSP and productId is pagopa";
    private static final String OTHER_NOTE_REQUIRED = "Other Note is required when other boolean are false";
    private static final String BILLING_OR_RECIPIENT_CODE_REQUIRED = "Billing and/or recipient code are required";

    public static Uni<Onboarding> customValidationOnboardingData(Onboarding onboarding) {
        if(InstitutionType.PA.equals(onboarding.getInstitution().getInstitutionType()) &&
        !ProductId.PROD_INTEROP.getValue().equals(onboarding.getProductId())){
            if(Objects.isNull(onboarding.getBilling()) || Objects.isNull(onboarding.getBilling().getRecipientCode()))
                return  Uni.createFrom().failure(new InvalidRequestException(BILLING_OR_RECIPIENT_CODE_REQUIRED));
        }
        if(InstitutionType.GSP == onboarding.getInstitution().getInstitutionType() &&
                ProductId.PROD_PAGOPA.getValue().equals(onboarding.getProductId())) {
            if(Objects.isNull(onboarding.getAdditionalInformations())) {
                return  Uni.createFrom().failure(new InvalidRequestException(ADDITIONAL_INFORMATIONS_REQUIRED));
            }
            else if (!onboarding.getAdditionalInformations().isIpa() &&
                    !onboarding.getAdditionalInformations().isBelongRegulatedMarket() &&
                    !onboarding.getAdditionalInformations().isEstablishedByRegulatoryProvision() &&
                    !onboarding.getAdditionalInformations().isAgentOfPublicService() &&
                    Objects.isNull(onboarding.getAdditionalInformations().getOtherNote())){
                return  Uni.createFrom().failure(new InvalidRequestException(OTHER_NOTE_REQUIRED));
            }
        }
        return Uni.createFrom().item(onboarding);
    }

}
