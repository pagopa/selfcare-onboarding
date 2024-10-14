package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.product.entity.Product;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;

import static it.pagopa.selfcare.onboarding.constants.CustomError.DEFAULT_ERROR;

public class RegistryManagerPT extends RegistryManagerIPAUo {

    public RegistryManagerPT(Onboarding onboarding, UoApi uoApi) {
        super(onboarding, uoApi);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return super.customValidation(product).onItem().invoke(() -> {
            if (!product.isDelegable()) {
                throw new OnboardingNotAllowedException(String.format(ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE,
                        onboarding.getInstitution().getTaxCode(),
                        onboarding.getProductId()), DEFAULT_ERROR.getCode());
            }
        });
    }
}
