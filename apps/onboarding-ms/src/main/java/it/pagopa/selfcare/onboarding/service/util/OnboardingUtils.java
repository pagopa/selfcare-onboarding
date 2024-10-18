package it.pagopa.selfcare.onboarding.service.util;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.Builder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.PDNDBusinessResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.util.Objects;

import static it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType.EC;
import static it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType.UO;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;
import static it.pagopa.selfcare.onboarding.constants.CustomError.*;

@ApplicationScoped
public class OnboardingUtils {

    @RestClient
    @Inject
    UoApi uoApi;

    @RestClient
    @Inject
    AooApi aooApi;

    @RestClient
    @Inject
    InfocamerePdndApi infocamerePdndApi;

    private static final String ADDITIONAL_INFORMATION_REQUIRED = "Additional Information is required when institutionType is GSP and productId is pagopa";
    private static final String OTHER_NOTE_REQUIRED = "Other Note is required when other boolean are false";
    private static final String BILLING_OR_RECIPIENT_CODE_REQUIRED = "Billing and/or recipient code are required";
    private static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE = "Institution with external id '%s' is not allowed to onboard '%s' product because it is not delegable";
    private static final String PARENT_TAX_CODE_IS_INVALID = "The tax code of the parent entity of the request does not match the tax code of the parent entity retrieved by IPA";
    private static final String TAX_CODE_INVOICING_IS_INVALID = "The tax code invoicing of the request does not match any tax code of institutions' hierarchy";

    public Uni<Onboarding> customValidationOnboardingData(Onboarding onboarding, Product product, ProxyResource proxyResource) {
            if (isUOWithSfe(onboarding)) {
                return checkParentTaxCode(onboarding, ((UOResource) proxyResource.getResource()).getCodiceFiscaleEnte())
                        .onItem().transformToUni(o -> checkTaxCodeInvoicing(onboarding, product));
            } else {
                return checkRecipientCode(onboarding, proxyResource)
                        .replaceWith(additionalChecksForProduct(onboarding, product));
            }
    }

    private Uni<Void> checkRecipientCode(Onboarding onboarding, ProxyResource proxyResource) {
        if (isInvoiceablePA(onboarding)) {
            final String recipientCode = onboarding.getBilling().getRecipientCode();
            return getUoFromRecipientCode(recipientCode)
                    .onItem().transformToUni(uoResource -> validationRecipientCode(onboarding, proxyResource, uoResource))
                    .onItem().transformToUni(customError -> {
                        if (Objects.nonNull(customError)) {
                            return Uni.createFrom().failure(new InvalidRequestException(customError.getMessage()));
                        }
                        return Uni.createFrom().nullItem();
                    });
        }
        return Uni.createFrom().nullItem();
    }

    private Uni<CustomError> validationRecipientCode(Onboarding onboarding, ProxyResource proxyResource, UOResource uoResource) {

        switch ((onboarding.getInstitution().getSubunitType() != null) ? onboarding.getInstitution().getSubunitType() : EC ) {
            case AOO -> {
                return  getValidationRecipientCodeError(((AOOResource) proxyResource.getResource()).getCodiceIpa(), uoResource);
            }
            case UO -> {
                return getValidationRecipientCodeError(((UOResource) proxyResource.getResource()).getCodiceIpa(), uoResource);
            }
            default -> {
                return getValidationRecipientCodeError(onboarding.getInstitution().getOriginId(), uoResource);
            }
        }
    }

    /**
     * Validate fields of onboarding in case of PRV or SCP
     * If digitalAddress or description does not match proxy data,
     * an exception is thrown
     */
    public Uni<Onboarding> validateFields(Onboarding onboarding) {
        if (InstitutionType.SCP == onboarding.getInstitution().getInstitutionType()
                || (InstitutionType.PRV == onboarding.getInstitution().getInstitutionType()
                        && !PROD_PAGOPA.getValue().equals(onboarding.getProductId()))) {
            return infocamerePdndApi.institutionPdndByTaxCodeUsingGET(onboarding.getInstitution().getTaxCode())
                    .onFailure(WebApplicationException.class)
                    .recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                            ? Uni.createFrom().failure(new ResourceNotFoundException(
                            String.format("Institution %s not found in the registry",
                                    onboarding.getInstitution().getTaxCode()
                            )))
                            : Uni.createFrom().failure(ex))
                    .onItem().transformToUni(pdndBusinessResource -> {
                        if (!originPDNDInfocamere(onboarding, pdndBusinessResource)) {
                            return Uni.createFrom().failure(new InvalidRequestException("Field digitalAddress or description are not valid"));
                        }
                        return Uni.createFrom().item(onboarding);
                    });
        }
        return Uni.createFrom().item(onboarding);
    }

    public Uni<UOResource> getUoFromRecipientCode(String recipientCode) {
        return uoApi.findByUnicodeUsingGET1(recipientCode, null)
                .onFailure(WebApplicationException.class)
                .recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(
                        String.format(UO_NOT_FOUND.getMessage(),
                                recipientCode
                        )))
                        : Uni.createFrom().failure(ex));
    }

    public Uni<CustomError> getValidationRecipientCodeError(String originIdEC, UOResource uoResource) {
        if (!originIdEC.equals(uoResource.getCodiceIpa())) {
            return Uni.createFrom().item(DENIED_NO_ASSOCIATION);
        }
        if (Objects.isNull(uoResource.getCodiceFiscaleSfe())) {
            return Uni.createFrom().item(DENIED_NO_BILLING);
        }
        return Uni.createFrom().nullItem();
    }

    private Uni<Void> checkParentTaxCode(Onboarding onboarding, String  childTaxCode) {
        /* if parent tax code is different from child tax code, throw an exception */
        if (!onboarding.getInstitution().getTaxCode().equals(childTaxCode)) {
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
                (!ProductId.PROD_INTEROP.getValue().equals(onboarding.getProductId()) && !PROD_PAGOPA.getValue().equals(onboarding.getProductId()))) {
            if (Objects.isNull(onboarding.getBilling()) || Objects.isNull(onboarding.getBilling().getRecipientCode()))
                return Uni.createFrom().failure(new InvalidRequestException(BILLING_OR_RECIPIENT_CODE_REQUIRED));
        }
        if (InstitutionType.GSP == onboarding.getInstitution().getInstitutionType() &&
                PROD_PAGOPA.getValue().equals(onboarding.getProductId())) {
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

    private boolean isUOWithSfe(Onboarding onboarding) {
        return Objects.nonNull(onboarding.getInstitution().getSubunitCode())
                && UO.equals(onboarding.getInstitution().getSubunitType())
                && Objects.nonNull(onboarding.getBilling())
                && Objects.nonNull(onboarding.getBilling().getTaxCodeInvoicing())
                && Objects.nonNull(onboarding.getInstitution().getTaxCode());
    }

    private boolean isInvoiceablePA(Onboarding onboarding) {
        return Objects.nonNull(onboarding.getInstitution())
                && InstitutionType.PA.equals(onboarding.getInstitution().getInstitutionType())
                && Objects.nonNull(onboarding.getBilling())
                && Objects.nonNull(onboarding.getBilling().getRecipientCode());
    }

    @Builder
    public static class ProxyResource<T> {
        private InstitutionPaSubunitType type;
        private T resource;
        public InstitutionPaSubunitType getType() { return type;}
        public void setType(InstitutionPaSubunitType type) { this.type = type; }
        public T getResource() { return resource; }
        public void setResource(T resource) { this.resource = resource; }
    }
  
    private boolean originPDNDInfocamere(Onboarding onboarding, PDNDBusinessResource pdndBusinessResource) {
        return onboarding.getInstitution().getDigitalAddress().equals(pdndBusinessResource.getDigitalAddress()) &&
                onboarding.getInstitution().getDescription().equals(pdndBusinessResource.getBusinessName());
    }
}
