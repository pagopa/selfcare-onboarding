package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import static it.pagopa.selfcare.onboarding.constants.CustomError.UO_NOT_FOUND;

public class WrapperUO extends Wrapper<Uni<UOResource>> {

    private UoApi client;

    public WrapperUO(Onboarding onboarding, UoApi uoApi) {
        super(onboarding);
        client = uoApi;
        registryResource = retrieveInstitution();
    }

    @Override
    public Uni<UOResource> retrieveInstitution() {
        return client.findByUnicodeUsingGET1(onboarding.getInstitution().getSubunitCode(), null)
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format(UO_NOT_FOUND.getMessage(), onboarding.getInstitution().getSubunitCode())))
                        : Uni.createFrom().failure(ex))
                .onItem().invoke(uoResource -> onboarding.getInstitution().setParentDescription(uoResource.getDenominazioneEnte()));
    }

    @Override
    boolean customValidation() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

}
