package it.pagopa.selfcare.onboarding.conf;

import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import jakarta.annotation.PostConstruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;

@Singleton
@Startup
public class CryptoConfig {
    @ConfigProperty(name = "onboarding-ms.data.encryption.key")
    String key;

    @PostConstruct
    void init() {
        DataEncryptionUtils.setDefaultKey(key);
    }
}