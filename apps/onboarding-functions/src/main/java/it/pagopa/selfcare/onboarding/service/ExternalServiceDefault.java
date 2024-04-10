package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.config.ExternalConfig;

public class ExternalServiceDefault implements ExternalService {

    private ExternalConfig externalConfig;

    public ExternalServiceDefault(ExternalConfig externalConfig) {
        this.externalConfig = externalConfig;
    }

    @Override
    public boolean checkOrganization(String productId, String fiscalCode, String vatNumber) {
        return false;
    }
}
