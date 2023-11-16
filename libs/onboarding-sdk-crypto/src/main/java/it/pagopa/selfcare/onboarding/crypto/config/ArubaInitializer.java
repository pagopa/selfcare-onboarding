package it.pagopa.selfcare.onboarding.crypto.config;

import it.pagopa.selfcare.onboarding.crypto.soap.aruba.sign.generated.client.Auth;

import java.util.Optional;

public class ArubaInitializer {

    private ArubaInitializer() {}

    public static ArubaSignConfig initializeConfig() {

        ArubaSignConfig config = new ArubaSignConfig();
        config.setConnectTimeoutMs(Optional.ofNullable(System.getProperty("aruba.sign-service.connectTimeoutMs"))
                .map(Integer::parseInt).orElse(0));
        config.setRequestTimeoutMs(Optional.ofNullable(System.getProperty("aruba.sign-service.requestTimeoutMs"))
                .map(Integer::parseInt).orElse(0));
        config.setBaseUrl(Optional.ofNullable(System.getProperty("aruba.sign-service.baseUrl"))
                .orElse("https://arss.demo.firma-automatica.it:443/ArubaSignService/ArubaSignService"));

        Auth auth = new Auth();
        auth.setTypeOtpAuth(System.getProperty("aruba.sign-service.auth.typeOtpAuth"));
        auth.setDelegatedDomain(System.getProperty("aruba.sign-service.auth.delegatedDomain"));
        auth.setOtpPwd(System.getProperty("aruba.sign-service.auth.otpPwd"));
        auth.setUser(System.getProperty("aruba.sign-service.auth.user"));
        auth.setDelegatedUser(System.getProperty("aruba.sign-service.auth.delegatedUser"));
        auth.setDelegatedPassword(System.getProperty("aruba.sign-service.auth.delegatedPassword"));
        auth.setDelegatedDomain(System.getProperty("aruba.sign-service.auth.delegatedDomain"));
        auth.setTypeHSM("COSIGN");
        config.setAuth(auth);

        return config;
    }
}
