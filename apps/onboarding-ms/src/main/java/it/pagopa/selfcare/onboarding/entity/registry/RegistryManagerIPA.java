package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.IPAEntity;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class RegistryManagerIPA extends RegistryManagerIPAUo {

    public RegistryManagerIPA(Onboarding onboarding, UoApi uoApi, InstitutionApi institutionApi) {
        super(onboarding, uoApi, institutionApi);
    }

    @Override
    public IPAEntity retrieveInstitution() {
        super.originIdEC = onboarding.getInstitution().getOriginId();
        InstitutionResource institutionResource =  super.institutionApi.findInstitutionUsingGET(onboarding.getInstitution().getTaxCode(), null, null)
                .onFailure().retry().atMost(MAX_NUMBER_ATTEMPTS)
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format("Institution with taxCode %s not found", onboarding.getInstitution().getTaxCode())))
                        : Uni.createFrom().failure(ex))
                .await().atMost(Duration.of(DURATION_TIMEOUT, ChronoUnit.SECONDS));
        return IPAEntity.builder().institutionResource(institutionResource).build();
    }

    @Override
    public Uni<Boolean> isValid() {
        if (!originIPA(onboarding, registryResource.getInstitutionResource())) {
            return Uni.createFrom().failure(new InvalidRequestException("Field digitalAddress or description are not valid"));
        }
        return Uni.createFrom().item(true);
    }

    private boolean originIPA(Onboarding onboarding, InstitutionResource institutionResource) {
        return onboarding.getInstitution().getDigitalAddress().equals(institutionResource.getDigitalAddress()) &&
                onboarding.getInstitution().getDescription().equals(institutionResource.getDescription());
    }

}
