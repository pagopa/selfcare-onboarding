package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;
import java.util.List;

public interface TokenService {

    Uni<List<Token>> getToken(String onboardingId);

    Uni<RestResponse<File>> retrieveContractNotSigned(String onboardingId);

    Uni<RestResponse<File>> retrieveAttachment(String onboardingId, String attachmentName);
}
