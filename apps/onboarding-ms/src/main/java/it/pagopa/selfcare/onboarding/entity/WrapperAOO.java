package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static it.pagopa.selfcare.onboarding.constants.CustomError.AOO_NOT_FOUND;

public class WrapperAOO extends WrapperUO {

    private final AooApi client;

    public WrapperAOO(Onboarding onboarding, AooApi aooApi, UoApi uoApi) {
        super(onboarding, uoApi);
        client = aooApi;
    }

    public IPAEntity retrieveInstitution() {
        AOOResource aooResource = client.findByUnicodeUsingGET(onboarding.getInstitution().getSubunitCode(), null)
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format(AOO_NOT_FOUND.getMessage(), onboarding.getInstitution().getSubunitCode())))
                        : Uni.createFrom().failure(ex))
                .onItem().invoke(this::enrichOnboardingData)
                      .await().atMost(Duration.of(5, ChronoUnit.SECONDS));
        return IPAEntity.builder().aooResource(aooResource).build();

    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return super.customValidation(product);
    }

    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }

    private void enrichOnboardingData(AOOResource aooResource) {
        onboarding.getInstitution().setParentDescription(aooResource.getDenominazioneEnte());
        onboarding.getInstitution().setIstatCode(aooResource.getCodiceComuneISTAT());
    }

}