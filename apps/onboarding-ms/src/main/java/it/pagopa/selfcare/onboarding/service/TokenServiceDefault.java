package it.pagopa.selfcare.onboarding.service;

import static it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT;
import static it.pagopa.selfcare.onboarding.common.TokenType.INSTITUTION;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.conf.OnboardingMsConfig;
import it.pagopa.selfcare.onboarding.entity.Token;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import org.jboss.resteasy.reactive.RestResponse;

@ApplicationScoped
public class TokenServiceDefault implements TokenService {
    private final AzureBlobClient azureBlobClient;
    private final OnboardingMsConfig onboardingMsConfig;

    public TokenServiceDefault(AzureBlobClient azureBlobClient, OnboardingMsConfig onboardingMsConfig) {
        this.azureBlobClient = azureBlobClient;
        this.onboardingMsConfig = onboardingMsConfig;
    }

    @Override
    public Uni<List<Token>> getToken(String onboardingId) {
        return Token.find("onboardingId", onboardingId)
                .list();
    }
    @Override
    public Uni<RestResponse<File>> retrieveContractNotSigned(String onboardingId) {
        return Token.find("onboardingId = ?1 and type = ?2", onboardingId, INSTITUTION.name())
                .firstResult()
                .map(Token.class::cast)
                .onItem().transformToUni(token ->
                            Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(String.format("%s%s/%s", onboardingMsConfig.getContractPath(), onboardingId, token.getContractFilename())))
                                    .runSubscriptionOn(Executors.newSingleThreadExecutor())
                                    .onItem().transform(contract -> {
                                        RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok(contract, MediaType.APPLICATION_OCTET_STREAM);
                                        response.header("Content-Disposition", "attachment;filename=" + token.getContractFilename());
                                        return response.build();
                                    }));
    }

    @Override
    public Uni<RestResponse<File>> retrieveAttachment(String onboardingId, String attachmentName) {
        return Token.find("onboardingId = ?1 and type = ?2 and name = ?3", onboardingId, ATTACHMENT.name(), attachmentName)
                .firstResult()
                .map(Token.class::cast)
                .onItem().transformToUni(token ->
                        Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(getAttachmentByOnboarding(onboardingId, token.getContractFilename())))
                                .runSubscriptionOn(Executors.newSingleThreadExecutor())
                                .onItem().transform(contract -> {
                                    RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok(contract, MediaType.APPLICATION_OCTET_STREAM);
                                    response.header("Content-Disposition", "attachment;filename=" + token.getContractFilename());
                                    return response.build();
                                }));
    }

    @Override
    public Uni<List<String>> getAttachments(String onboardingId) {
        return Token.find("onboardingId = ?1 and type = ?2", onboardingId, ATTACHMENT.name())
                .stream().onItem().transform(Token.class::cast)
                .map(Token::getName)
                .collect().asList();
    }

    private String getAttachmentByOnboarding(String onboardingId, String filename) {
        return String.format("%s%s%s%s",onboardingMsConfig.getContractPath(), onboardingId, "/attachments", "/" + filename);
    }
}
