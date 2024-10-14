package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.model.PDNDBusinessResource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public abstract class ClientRegistryPDNDInfocamere extends BaseRegistryManager<PDNDBusinessResource> {

    private final InfocamerePdndApi client;

    public ClientRegistryPDNDInfocamere(Onboarding onboarding, InfocamerePdndApi infocamerePdndApi) {
        super(onboarding);
        this.client = infocamerePdndApi;
    }

    public PDNDBusinessResource retrieveInstitution() {
        return client.institutionPdndByTaxCodeUsingGET(onboarding.getInstitution().getTaxCode())
                .onFailure(WebApplicationException.class)
                .recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(
                        String.format("Institution %s not found in the registry",
                                onboarding.getInstitution().getTaxCode()
                        )))
                        : Uni.createFrom().failure(ex))
                .await().atMost(Duration.of(DURATION_TIMEOUT, ChronoUnit.SECONDS));
    }
}