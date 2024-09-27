package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.util.Objects;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_INTEROP;
import static it.pagopa.selfcare.onboarding.constants.CustomError.*;
import static it.pagopa.selfcare.onboarding.service.util.OnboardingUtils.BILLING_OR_RECIPIENT_CODE_REQUIRED;

public class WrapperIPA extends BaseWrapper<Uni<IPAEntity>> {

    protected UoApi uoClient;
    private  final org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi client;

    public WrapperIPA(Onboarding onboarding, InstitutionApi institutionApi, UoApi uoApi) {
        super(onboarding);
        registryResource = retrieveInstitution();
        client = institutionApi;
        uoClient = uoApi;
    }

    @Override
    public Uni<IPAEntity> retrieveInstitution() {
        return client.findInstitutionUsingGET(onboarding.getInstitution().getId(), "", null)
                .onItem().transformToUni(institutionResource -> Uni.createFrom().item(IPAEntity.builder().institutionResource(institutionResource).build()));
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


    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }

    public Uni<UOResource> getUoFromRecipientCode(String recipientCode) {
        return uoClient.findByUnicodeUsingGET1(recipientCode, null)
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

    protected boolean isInvoiceablePA(Onboarding onboarding) {
        return Objects.nonNull(onboarding.getBilling())
                && Objects.nonNull(onboarding.getBilling().getRecipientCode());
    }
}
