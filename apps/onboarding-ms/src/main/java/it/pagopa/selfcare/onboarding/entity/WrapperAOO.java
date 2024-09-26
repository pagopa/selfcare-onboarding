package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;

import static it.pagopa.selfcare.onboarding.constants.CustomError.AOO_NOT_FOUND;

public class WrapperAOO extends BaseWrapper<Uni<AOOResource>> {

    private final AooApi client;

    public WrapperAOO(Onboarding onboarding, AooApi aooApi) {
        super(onboarding);
        client = aooApi;
        registryResource = retrieveInstitution();
    }

    public Uni<AOOResource> retrieveInstitution() {
        return client.findByUnicodeUsingGET(onboarding.getInstitution().getSubunitCode(), null)
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format(AOO_NOT_FOUND.getMessage(), onboarding.getInstitution().getSubunitCode())))
                        : Uni.createFrom().failure(ex))
                .onItem().invoke(aooResource ->  {
                    onboarding.getInstitution().setParentDescription(aooResource.getDenominazioneEnte());
                    onboarding.getInstitution().setIstatCode(aooResource.getCodiceComuneISTAT());
                });
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return Uni.createFrom().item(onboarding);
    }

    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }

}