package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "fd")
public interface FDConfig {
    boolean byPassCheckOrganization();

    String grantType();

    String clientId();

    String clientSecret();
}