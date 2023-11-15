package it.pagopa.selfcare.onboarding.crypto.config;

import it.pagopa.selfcare.onboarding.crypto.utils.CryptoUtils;
import it.pagopa.selfcare.onboarding.crypto.utils.PropertiesLoader;
import org.apache.commons.lang3.StringUtils;

import java.util.Properties;

public class LocalCryptoInitializer {

    public static LocalCryptoConfig initializeConfig() {
        Properties properties = PropertiesLoader.loadProperties("cryptoConfig.properties");
        LocalCryptoConfig config = new LocalCryptoConfig();
        String cert = properties.getProperty("crypto.key.cert");
        String pKey = properties.getProperty("crypto.key.private");

        try {
            if(StringUtils.isBlank(cert) || StringUtils.isBlank(pKey)){
                throw new IllegalStateException("Define private and cert values in order to perform locally sign operations");
            }

            config.setCertificate(CryptoUtils.getCertificate(cert));
            config.setPrivateKey(CryptoUtils.getPrivateKey(pKey));
        } catch (Exception e) {
            throw new IllegalStateException("Something gone wrong while loading crypto private and public keys", e);
        }

        return config;
    }
}
