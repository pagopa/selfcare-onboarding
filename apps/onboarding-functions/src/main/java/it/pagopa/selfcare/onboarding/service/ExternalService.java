package it.pagopa.selfcare.onboarding.service;

public interface ExternalService {

    boolean checkOrganization(String productId, String fiscalCode, String vatNumber);
}
