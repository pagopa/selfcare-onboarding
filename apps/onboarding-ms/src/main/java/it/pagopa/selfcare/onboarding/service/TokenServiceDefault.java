package it.pagopa.selfcare.onboarding.service;

import static it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.conf.OnboardingMsConfig;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.bson.Document;
import org.jboss.resteasy.reactive.RestResponse;

@ApplicationScoped
public class TokenServiceDefault implements TokenService {
    private final AzureBlobClient azureBlobClient;
    private final OnboardingMsConfig onboardingMsConfig;

    private static final String ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED =
        "Token with id %s not found or already deleted";

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
        return Token.find("onboardingId", onboardingId)
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
    public Uni<Long> updateContractSigned(String onboardingId, String documentSignedPath) {

        Map<String, Object> queryParameter = new HashMap<>();
        queryParameter.put("contractSigned", documentSignedPath);
        Document query = QueryUtils.buildUpdateDocument(queryParameter);

        return Token.update(query)
            .where("_id", onboardingId)
            .onItem()
            .transformToUni(
                updateItemCount -> {
                    if (updateItemCount == 0) {
                        return Uni.createFrom()
                            .failure(
                                new InvalidRequestException(
                                    String.format(ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED, onboardingId)));
                    }
                    return Uni.createFrom().item(updateItemCount);
                });
    }

    public Uni<List<String>> getAttachments(String onboardingId) {
        return Token.find("onboardingId = ?1 and type = ?2", onboardingId, ATTACHMENT.name())
            .stream().onItem().transform(Token.class::cast)
            .map(Token::getName)
            .collect().asList();
    }

    private String getAttachmentByOnboarding(String onboardingId, String filename) {
        return String.format("%s%s%s%s", onboardingMsConfig.getContractPath(), onboardingId, "/attachments", "/" + filename);
    }

}
