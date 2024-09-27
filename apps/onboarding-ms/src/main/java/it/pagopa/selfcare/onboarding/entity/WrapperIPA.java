package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.util.Objects;

import static it.pagopa.selfcare.onboarding.constants.CustomError.*;

public class WrapperIPA extends BaseWrapper<Uni<InstitutionResource>> {

    protected UoApi uoClient;
    private  final org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi client;

    public WrapperIPA(Onboarding onboarding, InstitutionApi institutionApi, UoApi uoApi) {
        super(onboarding);
        registryResource = retrieveInstitution();
        client = institutionApi;
        uoClient = uoApi;
    }

    @Override
    public Uni<InstitutionResource> retrieveInstitution() {
        return client.findInstitutionUsingGET(onboarding.getInstitution().getId(), "", null);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return checkRecipientCode().replaceWith(onboarding);
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
