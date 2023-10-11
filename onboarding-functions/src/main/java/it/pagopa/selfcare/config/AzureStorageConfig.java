package it.pagopa.selfcare.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.mail-template.placeholders.onboarding")
public interface AzureStorageConfig {

    String connectionString();
    String accountName();
    String endpointSuffix();
    String accountKey();

    String container();
    String contractPath();

    String checkoutTemplateContainer();
}
