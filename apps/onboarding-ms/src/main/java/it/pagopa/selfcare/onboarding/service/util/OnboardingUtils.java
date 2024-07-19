package it.pagopa.selfcare.onboarding.service.util;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.util.InstitutionPaSubunitType;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.util.Objects;

import static it.pagopa.selfcare.onboarding.constants.CustomError.*;

@ApplicationScoped
public class OnboardingUtils {

    @RestClient
    @Inject
    UoApi uoApi;

    private static final String ADDITIONAL_INFORMATION_REQUIRED = "Additional Information is required when institutionType is GSP and productId is pagopa";
    private static final String OTHER_NOTE_REQUIRED = "Other Note is required when other boolean are false";
    private static final String BILLING_OR_RECIPIENT_CODE_REQUIRED = "Billing and/or recipient code are required";
    private static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE = "Institution with external id '%s' is not allowed to onboard '%s' product because it is not delegable";
    private static final String PARENT_TAX_CODE_IS_INVALID = "The tax code of the parent entity of the request does not match the tax code of the parent entity retrieved by IPA";
    private static final String TAX_CODE_INVOICING_IS_INVALID = "The tax code invoicing of the request does not match any tax code of institutions' hierarchy";

    public Uni<Onboarding> customValidationOnboardingData(Onboarding onboarding, Product product) {
        if (isUO(onboarding)) {
            return uoApi.findByUnicodeUsingGET1(onboarding.getInstitution().getSubunitCode(), null)
                    .flatMap(uoResource -> checkParentTaxCode(onboarding, uoResource))
                    .onItem().transformToUni(o -> checkTaxCodeInvoicing(onboarding, product));
        }
        return checkRecipientCode(onboarding)
                .replaceWith(additionalChecksForProduct(onboarding, product));
    }

    private Uni<Void> checkRecipientCode(Onboarding onboarding) {
        if (Objects.nonNull(onboarding.getBilling())
                && Objects.nonNull(onboarding.getBilling().getRecipientCode())) {
            return uoApi.findByUnicodeUsingGET1(onboarding.getBilling().getRecipientCode(), null)
                    .flatMap(uoResource -> validationRecipientCode(onboarding, uoResource))
                    .onItem().transformToUni(customError -> {
                        if (Objects.nonNull(customError)) {
                            return Uni.createFrom().failure(new InvalidRequestException(customError.getMessage()));
                        }
                        return Uni.createFrom().nullItem();
                    });
        }
        return Uni.createFrom().nullItem();
    }

    private Uni<CustomError> validationRecipientCode(Onboarding onboarding, UOResource uoResource) {
        if (Objects.isNull(uoResource.getCodiceFiscaleSfe())) {
            return Uni.createFrom().item(DENIED_NO_BILLING);
        }
        if (!onboarding.getInstitution().getOriginId().equals(uoResource.getCodiceIpa())) {
            return Uni.createFrom().item(DENIED_NO_ASSOCIATION);
        }
        return Uni.createFrom().nullItem();
    }

    private Uni<Void> checkParentTaxCode(Onboarding onboarding, UOResource uoResource) {
        /* if parent tax code is different from child tax code, throw an exception */
        if (!onboarding.getInstitution().getTaxCode().equals(uoResource.getCodiceFiscaleEnte())) {
            return Uni.createFrom().failure(new InvalidRequestException(PARENT_TAX_CODE_IS_INVALID));
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Onboarding> checkTaxCodeInvoicing(Onboarding onboarding, Product product) {
        /* if tax code invoicing is not into hierarchy, throw an exception */
        return uoApi.findAllUsingGET1(null, null, onboarding.getBilling().getTaxCodeInvoicing())
                .flatMap(uosResource -> {
                    /* if parent tax code is not into hierarchy, throw an exception */
                    if (Objects.nonNull(uosResource) && Objects.nonNull(uosResource.getItems())
                            && uosResource.getItems().stream().anyMatch(uoResource -> !uoResource.getCodiceFiscaleEnte().equals(onboarding.getInstitution().getTaxCode())))
                    {
                        return Uni.createFrom().failure(new InvalidRequestException(TAX_CODE_INVOICING_IS_INVALID));
                    }
                    return additionalChecksForProduct(onboarding, product);
                });
    }

    private Uni<Onboarding> additionalChecksForProduct(Onboarding onboarding, Product product) {

        /* if PT and product is not delegable, throw an exception */
        if (InstitutionType.PT == onboarding.getInstitution().getInstitutionType() && !product.isDelegable()) {
            throw new OnboardingNotAllowedException(String.format(ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE,
                    onboarding.getInstitution().getTaxCode(),
                    onboarding.getProductId()), DEFAULT_ERROR.getCode());
        }
        if (InstitutionType.PA.equals(onboarding.getInstitution().getInstitutionType()) &&
                !ProductId.PROD_INTEROP.getValue().equals(onboarding.getProductId())) {
            if (Objects.isNull(onboarding.getBilling()) || Objects.isNull(onboarding.getBilling().getRecipientCode()))
                return Uni.createFrom().failure(new InvalidRequestException(BILLING_OR_RECIPIENT_CODE_REQUIRED));
        }
        if (InstitutionType.GSP == onboarding.getInstitution().getInstitutionType() &&
                ProductId.PROD_PAGOPA.getValue().equals(onboarding.getProductId())) {
            if (Objects.isNull(onboarding.getAdditionalInformations())) {
                return Uni.createFrom().failure(new InvalidRequestException(ADDITIONAL_INFORMATION_REQUIRED));
            } else if (!onboarding.getAdditionalInformations().isIpa() &&
                    !onboarding.getAdditionalInformations().isBelongRegulatedMarket() &&
                    !onboarding.getAdditionalInformations().isEstablishedByRegulatoryProvision() &&
                    !onboarding.getAdditionalInformations().isAgentOfPublicService() &&
                    Objects.isNull(onboarding.getAdditionalInformations().getOtherNote())) {
                return Uni.createFrom().failure(new InvalidRequestException(OTHER_NOTE_REQUIRED));
            }
        }
        return Uni.createFrom().item(onboarding);
    }

    private boolean isUO(Onboarding onboarding) {
        return Objects.nonNull(onboarding.getInstitution().getSubunitCode())
                && onboarding.getInstitution().getSubunitType().equals(InstitutionPaSubunitType.UO)
                && Objects.nonNull(onboarding.getBilling())
                && Objects.nonNull(onboarding.getBilling().getTaxCodeInvoicing())
                && Objects.nonNull(onboarding.getInstitution().getTaxCode());
    }
}
