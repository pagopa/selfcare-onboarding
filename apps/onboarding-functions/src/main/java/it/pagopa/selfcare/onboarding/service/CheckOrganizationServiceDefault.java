package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.client.fd.FDRestClient;
import it.pagopa.selfcare.onboarding.config.FDConfig;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.logging.Level;

@ApplicationScoped
public class CheckOrganizationServiceDefault implements CheckOrganizationService {
    @RestClient
    @Inject
    FDRestClient fdRestClient;

    private final FDConfig fdConfig;

    public CheckOrganizationServiceDefault(
            FDConfig fdConfig) {
        this.fdConfig = fdConfig;
    }

    @Override
    public boolean checkOrganization(ExecutionContext context, String fiscalCode, String vatNumber) {
        context.getLogger().info("checkOrganization start");
        context.getLogger().info(() -> String.format("checkOrganization fiscalCode = %s, vatNumber = %s", fiscalCode, vatNumber));

        if (fdConfig.byPassCheckOrganization()) {
            context.getLogger().info("byPassCheckOrganization is true, skipping check");
            return false;
        } else {
            boolean alreadyRegistered;
            try {
                context.getLogger().info("byPassCheckOrganization is false, performing check");
                alreadyRegistered = fdRestClient.checkOrganization(fiscalCode, vatNumber).isAlreadyRegistered();
                context.getLogger().info(() -> String.format("checkOrganization result = %s", alreadyRegistered));
                context.getLogger().info("checkOrganization end");
                return alreadyRegistered;
            } catch (Exception e) {
                context.getLogger().log(Level.WARNING, e, () -> "An error occurred while checking the organization");
                throw new NotificationException(String.format("Error during organization check: %s", e.getMessage()));
            }
        }
    }
}
