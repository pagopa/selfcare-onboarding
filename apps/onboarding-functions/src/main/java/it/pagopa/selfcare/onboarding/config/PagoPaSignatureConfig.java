package it.pagopa.selfcare.onboarding.config;


import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-functions.pagopa-signature")
public interface PagoPaSignatureConfig {

    String source();

    String signer();

    String location();

    boolean applyOnboardingEnabled();

    String applyOnboardingTemplateReason();

    String euListOfTrustedListsURL();

    String euOfficialJournalUrl();
}
