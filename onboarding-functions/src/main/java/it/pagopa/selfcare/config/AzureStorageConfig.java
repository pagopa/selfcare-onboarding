package it.pagopa.selfcare.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.blob-storage")
public interface AzureStorageConfig {

    String connectionStringContract();
    String connectionStringProduct();
    String containerContract();
    String containerProduct();
    String contractPath();
    String productFilepath();

}
