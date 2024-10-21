package it.pagopa.selfcare.onboarding.service.util;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
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
