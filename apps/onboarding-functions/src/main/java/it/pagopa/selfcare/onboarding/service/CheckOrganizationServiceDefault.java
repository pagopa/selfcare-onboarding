package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.config.FDConfig;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CheckOrganizationServiceDefault implements CheckOrganizationService {

    private final FDConfig fdConfig;

    public CheckOrganizationServiceDefault(FDConfig fdConfig) {
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
            // TODO: implement the check via API
            return true;
        }
    }
}
