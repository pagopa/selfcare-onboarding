package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.util.Objects;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_INTEROP;
import static it.pagopa.selfcare.onboarding.constants.CustomError.*;

public class RegistryManagerIPAUo extends ClientRegistryIPA {

    public RegistryManagerIPAUo(Onboarding onboarding, UoApi uoApi, AooApi aooApi) {
        super(onboarding, uoApi, aooApi);
    }

    public RegistryManagerIPAUo(Onboarding onboarding, UoApi uoApi) {
        super(onboarding, uoApi);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return checkRecipientCode().onItem().transformToUni(unused -> {
            if (!PROD_INTEROP.getValue().equals(onboarding.getProductId())
                    && Objects.isNull(onboarding.getBilling()) || Objects.isNull(onboarding.getBilling().getRecipientCode())) {
                return Uni.createFrom().failure(new InvalidRequestException(BILLING_OR_RECIPIENT_CODE_REQUIRED));
            }
            return Uni.createFrom().item(onboarding);
        }).onItem().transformToUni(unused -> billingChecks());
    }

    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }

    public Uni<UOResource> getUoFromRecipientCode(String recipientCode) {
        return super.uoClient.findByUnicodeUsingGET1(recipientCode, null)
                .onFailure(WebApplicationException.class)
                .recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(
                        String.format(UO_NOT_FOUND.getMessage(),
                                recipientCode
                        )))
                        : Uni.createFrom().failure(ex));
    }

    protected Uni<Void> checkRecipientCode() {
        if (isInvoiceablePA(onboarding)) {
            final String recipientCode = onboarding.getBilling().getRecipientCode();
            return getUoFromRecipientCode(recipientCode)
                    .onItem().transformToUni(this::validationRecipientCode)
                    .onItem().transformToUni(customError -> {
                        if (Objects.nonNull(customError)) {
                            return Uni.createFrom().failure(new InvalidRequestException(customError.getMessage()));
                        }
                        return Uni.createFrom().nullItem();
                    });
        }
        return Uni.createFrom().nullItem();
    }

    protected Uni<CustomError> validationRecipientCode(UOResource uoResource) {
            if (Objects.nonNull(originIdEC) && !originIdEC.equals(uoResource.getCodiceIpa())) {
                return Uni.createFrom().item(DENIED_NO_ASSOCIATION);
            }
            if (Objects.isNull(uoResource.getCodiceFiscaleSfe())) {
                return Uni.createFrom().item(DENIED_NO_BILLING);
            }
            return Uni.createFrom().nullItem();
    }

    protected boolean isInvoiceablePA(Onboarding onboarding) {
        return Objects.nonNull(onboarding.getBilling())
                && Objects.nonNull(onboarding.getBilling().getRecipientCode());
    }

    private boolean hasSfe(Onboarding onboarding) {
        return Objects.nonNull(onboarding.getBilling())
                && Objects.nonNull(onboarding.getBilling().getTaxCodeInvoicing())
                && Objects.nonNull(onboarding.getInstitution().getTaxCode());
    }

    private Uni<Void> checkParentTaxCode() {
        /* if parent tax code is different from child tax code, throw an exception */
        if (!onboarding.getInstitution().getTaxCode().equals(resourceTaxCode)) {
            return Uni.createFrom().failure(new InvalidRequestException(PARENT_TAX_CODE_IS_INVALID));
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Onboarding> checkTaxCodeInvoicing(Onboarding onboarding) {
        /* if tax code invoicing is not into hierarchy, throw an exception */
        return super.uoClient.findAllUsingGET1(null, null, onboarding.getBilling().getTaxCodeInvoicing())
                .flatMap(uosResource -> {
                    /* if parent tax code is not into hierarchy, throw an exception */
                    if (Objects.nonNull(uosResource) && Objects.nonNull(uosResource.getItems())
                            && uosResource.getItems().stream().anyMatch(uoResource -> !uoResource.getCodiceFiscaleEnte().equals(onboarding.getInstitution().getTaxCode())))
                    {
                        return Uni.createFrom().failure(new InvalidRequestException(TAX_CODE_INVOICING_IS_INVALID));
                    }
                    return Uni.createFrom().item(onboarding);
                });
    }

    private Uni<Onboarding> billingChecks() {
        if (hasSfe(onboarding)) {
            return checkParentTaxCode()
                    .onItem().transformToUni(ignored -> checkTaxCodeInvoicing(onboarding));
        }
        return Uni.createFrom().item(onboarding);
    }
}
