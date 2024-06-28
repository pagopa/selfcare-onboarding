package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.io.File;
import java.util.List;
import java.util.Optional;

public interface ContractService {
    File createContractPDF(String contractTemplatePath, Onboarding onboarding, UserResource manager, List<UserResource> users, String productName, String pdfFormatFilename);

    File loadContractPDF(String contractTemplatePath, String onboardingId, String productName);
    File retrieveContractNotSigned(OnboardingWorkflow onboardingWorkflow, String productName);

    Optional<File> getLogoFile();
}
