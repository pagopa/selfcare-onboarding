package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.conf.OnboardingMsConfig;
import it.pagopa.selfcare.onboarding.entity.Token;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;

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
    public Uni<RestResponse<File>> retrieveAggregatesCsv(String onboardingId, String productId) {
        return Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(String.format("%s%s/%s/%s", onboardingMsConfig.getAggregatesPath(), onboardingId, productId, "aggregates.csv")))
                                .runSubscriptionOn(Executors.newSingleThreadExecutor())
                                .onItem().transform(csv -> {
                                    RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok(csv, MediaType.APPLICATION_OCTET_STREAM);
                                    response.header("Content-Disposition", "attachment;filename= aggregates.csv");
                                    return response.build();
                                });
    }
}
