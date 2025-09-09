package it.pagopa.selfcare.onboarding.config;

import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
@Startup
@Slf4j
public class DataEncryptionConfig {

    @ConfigProperty(name = "onboarding-fn.data.encryption.key")
    String key;

    @PostConstruct
    void init() {
        log.info("Adding key from kv size: {} is-empty-string: {}", key.length(), StringUtils.isBlank(key));
        DataEncryptionUtils.setDefaultKey(key);
    }
}