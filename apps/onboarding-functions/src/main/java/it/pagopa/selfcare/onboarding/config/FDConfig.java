package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "fd")
public interface FDConfig {
    boolean byPassCheckOrganization();
}