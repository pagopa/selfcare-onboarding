package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.client.external.ExternalRestClient;
import it.pagopa.selfcare.onboarding.config.ExternalConfig;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class CheckOrganizationServiceDefault implements CheckOrganizationService {
    @RestClient
    @Inject
    ExternalRestClient externalRestClient;

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
}
