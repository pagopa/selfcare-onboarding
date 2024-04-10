package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.external")
public interface ExternalConfig {

    String prodFdBypassCheckOrganization();
    String prodFdBaseUrl();
}
