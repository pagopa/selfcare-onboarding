package it.pagopa.selfcare.onboarding.config;

import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
@Startup
public class DataEncryptionConfig {

    @ConfigProperty(name = "onboarding-fn.data.encryption.key")
    String key;

    @PostConstruct
    void init() {
        DataEncryptionUtils.setDefaultKey(key);
    }
}