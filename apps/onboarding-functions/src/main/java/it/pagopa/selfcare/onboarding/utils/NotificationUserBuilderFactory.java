package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;

import static it.pagopa.selfcare.onboarding.entity.Topic.*;

@ApplicationScoped
public class NotificationUserBuilderFactory {
    @Inject
    @RestClient
    InstitutionApi proxyRegistryInstitutionApi;
    @Inject
    @RestClient
    GeographicTaxonomiesApi geographicTaxonomiesApi;
    @Inject
    @RestClient
    UoApi proxyRegistryUoApi;
    @Inject
    @RestClient
    AooApi proxyRegistryAooApi;
    @Inject
    @RestClient
    org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi;
    private final String alternativeEmail;

    public NotificationUserBuilderFactory(@ConfigProperty(name = "onboarding-functions.sender-mail") String alternativeEmail) {
        this.alternativeEmail = alternativeEmail;
    }

    public NotificationBuilder create(NotificationConfig.Consumer consumer) {
        if (SC_CONTRACTS_FD.getValue().equalsIgnoreCase(consumer.topic())) {
            return new FdNotificationBuilder(this.alternativeEmail, consumer, proxyRegistryInstitutionApi, geographicTaxonomiesApi, coreInstitutionApi);
        }
        return null;
    }

}
