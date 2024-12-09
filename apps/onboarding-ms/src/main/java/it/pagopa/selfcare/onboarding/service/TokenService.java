package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Token;
import java.io.File;
import java.util.List;
import org.jboss.resteasy.reactive.RestResponse;

public interface TokenService {

    Uni<List<Token>> getToken(String onboardingId);

    Uni<RestResponse<File>> retrieveContractNotSigned(String onboardingId);

    Uni<RestResponse<File>> retrieveAttachment(String onboardingId, String attachmentName);

    Uni<Long> updateContractSigned(String onboardingId, String documentSignedPath);

    Uni<List<String>> getAttachments(String onboardingId);
}
