package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.response.ContractSignedReport;
import it.pagopa.selfcare.onboarding.entity.Token;

import java.io.File;
import java.util.List;

import org.jboss.resteasy.reactive.RestResponse;

public interface TokenService {

  Uni<List<Token>> getToken(String onboardingId);

  Uni<RestResponse<File>> retrieveContract(String onboardingId, boolean isSigned);

  Uni<RestResponse<File>> retrieveSignedFile(String onboardingId);

  public Uni<RestResponse<File>> retrieveAttachment(String onboardingId, String attachmentName);

  Uni<Long> updateContractSigned(String onboardingId, String documentSignedPath);

  Uni<List<String>> getAttachments(String onboardingId);

  Uni<ContractSignedReport> reportContractSigned(String onboardingId);

  Uni<RestResponse<Long>> deleteContract(String onboardingId);
}
