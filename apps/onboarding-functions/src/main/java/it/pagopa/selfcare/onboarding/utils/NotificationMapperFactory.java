package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.mapper.NotificationMapper;
import it.pagopa.selfcare.onboarding.mapper.impl.NotificationCommonMapper;
import it.pagopa.selfcare.onboarding.mapper.impl.NotificationFdMapper;
import it.pagopa.selfcare.onboarding.mapper.impl.NotificationSapMapper;
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
public class NotificationMapperFactory {
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

    public NotificationMapperFactory(@ConfigProperty(name = "onboarding-functions.sender-mail") String alternativeEmail) {
        this.alternativeEmail = alternativeEmail;
    }

    public NotificationMapper create(String topic) {
        NotificationMapper notificationMapper;

        if (SC_CONTRACTS_FD.getValue().equalsIgnoreCase(topic)) {
            notificationMapper = new NotificationFdMapper(this.alternativeEmail, proxyRegistryInstitutionApi, geographicTaxonomiesApi, coreInstitutionApi);
        } else if (SC_CONTRACTS_SAP.getValue().equalsIgnoreCase(topic)) {
            notificationMapper = new NotificationSapMapper(this.alternativeEmail, proxyRegistryInstitutionApi, geographicTaxonomiesApi, proxyRegistryUoApi, proxyRegistryAooApi, coreInstitutionApi);
        } else if (SC_CONTRACTS.getValue().equalsIgnoreCase(topic)) {
            notificationMapper = new NotificationCommonMapper(this.alternativeEmail, proxyRegistryInstitutionApi, geographicTaxonomiesApi, coreInstitutionApi);
        } else {
            throw new IllegalArgumentException("Topic not supported");
        }

        return notificationMapper;

    }

}
