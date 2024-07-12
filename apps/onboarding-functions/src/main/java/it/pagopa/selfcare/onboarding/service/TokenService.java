package it.pagopa.selfcare.onboarding.service;

public interface TokenService {

    String createJwt(String userId);
}
