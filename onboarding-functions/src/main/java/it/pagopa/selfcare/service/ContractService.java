package it.pagopa.selfcare.service;

import it.pagopa.selfcare.entity.Onboarding;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.io.File;
import java.util.List;

public interface ContractService {
    File createContractPDF(String contractTemplatePath, Onboarding onboarding, UserResource validManager, List<UserResource> users, List<String> geographicTaxonomies);

    File loadContractPDF(String contractTemplatePath, String onboardingId);
    File retrieveContractNotSigned(String onboardingId);
}
