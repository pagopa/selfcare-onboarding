package it.pagopa.selfcare.service;

import it.pagopa.selfcare.entity.Onboarding;
import it.pagopa.selfcare.util.InstitutionType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OnboardingServiceDefault implements OnboardingService {
    @Override
    public void onboarding(Onboarding onboardingRequest) {

        /** Check if Product is Valid and retrieve */
        /* PT is delegable ?
        if(InstitutionType.PT == onboardingData.getInstitutionType() && !delegable) {
            throw new OnboardingNotAllowedException(String.format(ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE,
                onboardingData.getTaxCode(),
                onboardingData.getProductId()));
        }*/

        /* Check validation on onboarding maps */
        /* Check if role user is valid using Product,
        admit only List.of(PartyRole.MANAGER, PartyRole.DELEGATE) or List.of(PartyRole.MANAGER) for PG */
        /* Verify already onboarding for product and product parent */
    }
}
