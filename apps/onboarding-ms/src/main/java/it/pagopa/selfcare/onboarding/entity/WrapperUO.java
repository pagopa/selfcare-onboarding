package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.util.Objects;

import static it.pagopa.selfcare.onboarding.constants.CustomError.UO_NOT_FOUND;

public class WrapperUO extends WrapperIPA {

    public WrapperUO(Onboarding onboarding, InstitutionApi institutionApi, UoApi uoApi) {
        super(onboarding, institutionApi, uoApi);
        registryResource = retrieveInstitution();
    }

    @Override
    public Uni<IPAEntity> retrieveInstitution() {
        return super.uoClient.findByUnicodeUsingGET1(onboarding.getInstitution().getSubunitCode(), null)
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format(UO_NOT_FOUND.getMessage(), onboarding.getInstitution().getSubunitCode())))
                        : Uni.createFrom().failure(ex))
                .onItem().invoke(this::enrichOnboardingData)
                .onItem().transformToUni(uoResource -> Uni.createFrom().item(IPAEntity.builder().uoResource(uoResource).build()));
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return super.customValidation(product).onItem().transformToUni(unused -> {
            if (hasSfe(onboarding)) {
                return checkParentTaxCode()
                        .onItem().transformToUni(ignored -> checkTaxCodeInvoicing(onboarding));
            }
            return Uni.createFrom().item(onboarding);
        });
    }

    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }

    private boolean hasSfe(Onboarding onboarding) {
        return Objects.nonNull(onboarding.getBilling())
                && Objects.nonNull(onboarding.getBilling().getTaxCodeInvoicing())
                && Objects.nonNull(onboarding.getInstitution().getTaxCode());
    }

    private Uni<Void> checkParentTaxCode() {
        /* if parent tax code is different from child tax code, throw an exception */
        return registryResource.onItem().transformToUni(ipaEntity -> {
            final String taxCode = ipaEntity.getUoResource().getCodiceFiscaleEnte();
            if (!onboarding.getInstitution().getTaxCode().equals(taxCode)) {
                return Uni.createFrom().failure(new InvalidRequestException(PARENT_TAX_CODE_IS_INVALID));
            }
            return Uni.createFrom().voidItem();
        });
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
                    //return additionalChecksForProduct(onboarding, product);
                    return Uni.createFrom().item(onboarding);
                });
    }

    private void enrichOnboardingData(UOResource uoResource) {
        onboarding.getInstitution().setParentDescription(uoResource.getDenominazioneEnte());
        onboarding.getInstitution().setIstatCode(uoResource.getCodiceComuneISTAT());
    }

}
