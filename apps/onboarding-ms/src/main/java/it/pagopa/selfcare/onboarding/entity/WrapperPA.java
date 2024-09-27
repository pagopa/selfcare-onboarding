package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.util.Objects;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_INTEROP;
import static it.pagopa.selfcare.onboarding.constants.CustomError.DENIED_NO_ASSOCIATION;
import static it.pagopa.selfcare.onboarding.constants.CustomError.DENIED_NO_BILLING;
import static it.pagopa.selfcare.onboarding.service.util.OnboardingUtils.BILLING_OR_RECIPIENT_CODE_REQUIRED;

public class WrapperPA extends WrapperIPA {

    public WrapperPA(Onboarding onboarding, InstitutionApi institutionApi, UoApi uoApi) {
        super(onboarding, institutionApi, uoApi);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
       return super.customValidation(product).onItem().transformToUni(unused -> {
           if (!PROD_INTEROP.getValue().equals(onboarding.getProductId())
                   && Objects.isNull(onboarding.getBilling()) || Objects.isNull(onboarding.getBilling().getRecipientCode())) {
               return Uni.createFrom().failure(new InvalidRequestException(BILLING_OR_RECIPIENT_CODE_REQUIRED));
           }
           return Uni.createFrom().item(onboarding);
       });
    }
}
