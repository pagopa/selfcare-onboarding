package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.io.File;
import java.util.List;

public interface ContractService {
    File createContractPDF(String contractTemplatePath, Onboarding onboarding, UserResource manager, List<UserResource> users, String productName);

    File loadContractPDF(String contractTemplatePath, String onboardingId, String productName);
    File retrieveContractNotSigned(String onboardingId, String productName);
}
