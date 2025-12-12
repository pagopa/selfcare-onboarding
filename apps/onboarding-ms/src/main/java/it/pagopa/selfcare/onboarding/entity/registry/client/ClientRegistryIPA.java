package it.pagopa.selfcare.onboarding.entity.registry.client;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.registry.BaseRegistryManager;
import it.pagopa.selfcare.onboarding.entity.IPAEntity;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.AooControllerApi;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesControllerApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionControllerApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoControllerApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static it.pagopa.selfcare.onboarding.constants.CustomError.UO_NOT_FOUND;

public abstract class ClientRegistryIPA extends BaseRegistryManager<IPAEntity> {

    protected InstitutionControllerApi institutionApi;
    protected final UoControllerApi uoClient;
    protected AooControllerApi aooClient;
    protected GeographicTaxonomiesControllerApi geographicTaxonomiesApi;
    protected String originIdEC;
    protected String resourceTaxCode;

    protected ClientRegistryIPA(Onboarding onboarding, UoControllerApi uoApi, AooControllerApi aooApi) {
        super(onboarding);
        this.uoClient = uoApi;
        this.aooClient = aooApi;
    }

    protected ClientRegistryIPA(Onboarding onboarding, UoControllerApi uoApi) {
        super(onboarding);
        this.uoClient = uoApi;
    }

    protected ClientRegistryIPA(Onboarding onboarding, UoControllerApi uoApi, InstitutionControllerApi institutionApi, GeographicTaxonomiesControllerApi geographicTaxonomiesApi) {
        super(onboarding);
        this.uoClient = uoApi;
        this.institutionApi = institutionApi;
        this.geographicTaxonomiesApi = geographicTaxonomiesApi;
    }

    public IPAEntity retrieveInstitution() {
        UOResource uoResource =  uoClient.findUoByUnicodeUsingGET(onboarding.getInstitution().getSubunitCode(), null)
                .onFailure().retry().atMost(MAX_NUMBER_ATTEMPTS)
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format(UO_NOT_FOUND.getMessage(), onboarding.getInstitution().getSubunitCode())))
                        : Uni.createFrom().failure(ex))
                .onItem().invoke(this::enrichOnboardingData)
                .await().atMost(Duration.of(DURATION_TIMEOUT, ChronoUnit.SECONDS));
        originIdEC = uoResource.getCodiceIpa();
        resourceTaxCode = uoResource.getCodiceFiscaleEnte();
        return IPAEntity.builder().uoResource(uoResource).build();
    }

    protected void enrichOnboardingData(UOResource uoResource) {
        onboarding.getInstitution().setParentDescription(uoResource.getDenominazioneEnte());
        onboarding.getInstitution().setIstatCode(uoResource.getCodiceComuneISTAT());
    }

}
