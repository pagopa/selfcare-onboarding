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
       return checkRecipientCode().onItem().transformToUni(unused -> {
           if (!PROD_INTEROP.getValue().equals(onboarding.getProductId())
                   && Objects.isNull(onboarding.getBilling()) || Objects.isNull(onboarding.getBilling().getRecipientCode())) {
               return Uni.createFrom().failure(new InvalidRequestException(BILLING_OR_RECIPIENT_CODE_REQUIRED));
           }
           return Uni.createFrom().item(onboarding);
       });
    }

    private boolean isInvoiceablePA(Onboarding onboarding) {
        return Objects.nonNull(onboarding.getBilling())
                && Objects.nonNull(onboarding.getBilling().getRecipientCode());
    }

    private Uni<Void> checkRecipientCode() {
        if (isInvoiceablePA(onboarding)) {
            final String recipientCode = onboarding.getBilling().getRecipientCode();
            return getUoFromRecipientCode(recipientCode)
                    .onItem().transformToUni(uoResource -> validationRecipientCode(uoResource))
                    .onItem().transformToUni(customError -> {
                        if (Objects.nonNull(customError)) {
                            return Uni.createFrom().failure(new InvalidRequestException(customError.getMessage()));
                        }
                        return Uni.createFrom().nullItem();
                    });
        }
        return Uni.createFrom().nullItem();
    }


    private Uni<CustomError> validationRecipientCode(UOResource uoResource) {
        return registryResource.onItem().transformToUni(registryResource -> {
            final String originIdEC = uoResource.getCodiceIpa();
            if (!originIdEC.equals(uoResource.getCodiceIpa())) {
                return Uni.createFrom().item(DENIED_NO_ASSOCIATION);
            }
            if (Objects.isNull(uoResource.getCodiceFiscaleSfe())) {
                return Uni.createFrom().item(DENIED_NO_BILLING);
            }
            return Uni.createFrom().nullItem();
        });
    }

}
