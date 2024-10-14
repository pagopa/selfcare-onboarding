package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.StationsApi;
import org.openapi.quarkus.party_registry_proxy_json.model.StationResource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static it.pagopa.selfcare.onboarding.constants.CustomError.AOO_NOT_FOUND;

public class WrapperANAC extends BaseWrapper<StationResource> {

    private final StationsApi client;

    public WrapperANAC(Onboarding onboarding, StationsApi stationsApi) {
        super(onboarding);
        client = stationsApi;
    }

    public StationResource retrieveInstitution() {
        return client.searchByTaxCodeUsingGET1(onboarding.getInstitution().getTaxCode())
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format(AOO_NOT_FOUND.getMessage(), onboarding.getInstitution().getSubunitCode())))
                        : Uni.createFrom().failure(ex))
                .await().atMost(Duration.of(5, ChronoUnit.SECONDS));
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