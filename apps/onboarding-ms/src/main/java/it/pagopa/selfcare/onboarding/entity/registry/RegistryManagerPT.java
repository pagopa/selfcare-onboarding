package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.product.entity.Product;

import static it.pagopa.selfcare.onboarding.constants.CustomError.DEFAULT_ERROR;

public class RegistryManagerPT extends RegistryManagerSELC {

    public RegistryManagerPT(Onboarding onboarding) {
        super(onboarding);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        if (!product.isDelegable()) {
            throw new OnboardingNotAllowedException(String.format(BaseRegistryManager.ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE,
                    onboarding.getInstitution().getTaxCode(),
                    onboarding.getProductId()), DEFAULT_ERROR.getCode());
        }
        return Uni.createFrom().item(onboarding);
    }
}
