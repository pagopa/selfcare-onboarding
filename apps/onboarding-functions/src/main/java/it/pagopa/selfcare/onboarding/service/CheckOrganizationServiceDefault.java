package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.client.external.ExternalRestClient;
import it.pagopa.selfcare.onboarding.client.external.ExternalTokenRestClient;
import it.pagopa.selfcare.onboarding.config.ExternalConfig;
import it.pagopa.selfcare.onboarding.dto.OauthToken;
import it.pagopa.selfcare.onboarding.dto.OrganizationLightBeanResponse;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Form;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class CheckOrganizationServiceDefault implements CheckOrganizationService {
    @RestClient
    @Inject
    ExternalRestClient externalRestClient;

    @RestClient
    @Inject
    ExternalTokenRestClient externalTokenRestClient;

    private final ExternalConfig externalConfig;

    public CheckOrganizationServiceDefault(ExternalConfig externalConfig) {
        this.externalConfig = externalConfig;
    }

    @Override
    public boolean checkOrganization(ExecutionContext context, String fiscalCode, String vatNumber) {
        context.getLogger().info("checkOrganization start");
        context.getLogger().info(() -> String.format("checkOrganization fiscalCode = %s, vatNumber = %s", fiscalCode, vatNumber));

        if (externalConfig.byPassCheckOrganization()) {
            context.getLogger().info("byPassCheckOrganization is true, skipping check");
            return false;
        } else {
            boolean alreadyRegistered;
            try {
                context.getLogger().info("byPassCheckOrganization is false, performing check");
                alreadyRegistered = externalRestClient.checkOrganization(fiscalCode, vatNumber).isAlreadyRegistered();
                context.getLogger().info(() -> String.format("checkOrganization result = %s", alreadyRegistered));
                context.getLogger().info("checkOrganization end");
                return alreadyRegistered;
            } catch (Exception e) {
                throw new NotificationException(String.format("Error during organization check: %s", e.getMessage()));
            }
        }
    }

    @Override
    public String testToken(ExecutionContext context) {
        context.getLogger().info("testToken start");

        Form form = buildTokenEntity();
        context.getLogger().info(String.format("testToken calling external service with form %s", form.asMap()));

        OauthToken oauthToken = externalTokenRestClient.getToken(buildTokenEntity());
        context.getLogger().info(() -> String.format("testToken response %s", oauthToken));
        context.getLogger().info("testToken end");
        return oauthToken.getAccessToken();
    }

    @Override
    public boolean testCheckOrganization(ExecutionContext context, String fiscalCode, String vatNumber) {
        context.getLogger().info("testCheckOrganization start");

        Form form = buildTokenEntity();
        context.getLogger().info(String.format("testToken calling external service with form %s", form.asMap()));

        OauthToken oauthToken = externalTokenRestClient.getToken(buildTokenEntity());
        context.getLogger().info(() -> String.format("getToken response %s", oauthToken.toString()));

        context.getLogger().info("performing checkOrganization");
        OrganizationLightBeanResponse response = externalRestClient.checkOrganization2(fiscalCode, vatNumber, "Bearer " + oauthToken.getAccessToken());
        context.getLogger().info(() -> String.format("checkOrganization2 result = %s", response.toString()));
        context.getLogger().info("testCheckOrganization end");
        return response.isAlreadyRegistered();
    }

    private Form buildTokenEntity() {
        Form form = new Form();
        form.param("grant_type", externalConfig.grantType());
        form.param("client_id", externalConfig.clientId());
        form.param("client_secret", externalConfig.clientSecret());
        return form;
    }
}
