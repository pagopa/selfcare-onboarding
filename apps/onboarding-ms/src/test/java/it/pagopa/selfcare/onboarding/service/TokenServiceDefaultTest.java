package it.pagopa.selfcare.onboarding.service;

import static it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.entity.Token;
import jakarta.inject.Inject;
import java.io.File;
import java.util.List;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class TokenServiceDefaultTest {

    @Inject
    TokenServiceDefault tokenService;
    @InjectMock
    AzureBlobClient azureBlobClient;

    private static final String onboardingId = "onboardingId";

    @Test
    void getToken() {
        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.list()).thenReturn(Uni.createFrom().item(List.of(new Token())));

        PanacheMock.mock(Token.class);
        when(Token.find("onboardingId", onboardingId))
                .thenReturn(queryPage);

        tokenService.getToken(onboardingId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();
    }

    @Test
    void retrieveContractNotSigned() {
        Token token = new Token();
        token.setContractFilename("fileName");
        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.firstResult()).thenReturn(Uni.createFrom().item(token));

        PanacheMock.mock(Token.class);
        when(Token.find("onboardingId", onboardingId))
                .thenReturn(queryPage);

        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(new File("fileName"));

        UniAssertSubscriber<RestResponse<File>> subscriber = tokenService.retrieveContractNotSigned(onboardingId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        RestResponse<File> actual = subscriber.awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
    }

    @Test
    void retrieveAttachment() {
        final String filename = "filename";
        Token token = new Token();
        token.setContractFilename(filename);
        token.setName(filename);
        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.firstResult()).thenReturn(Uni.createFrom().item(token));

        PanacheMock.mock(Token.class);
        when(Token.find("onboardingId = ?1 and type = ?2 and name = ?3", onboardingId, ATTACHMENT.name(), filename))
        .thenReturn(queryPage);

        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(new File("fileName"));

        UniAssertSubscriber<RestResponse<File>> subscriber = tokenService.retrieveAttachment(onboardingId, filename)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        RestResponse<File> actual = subscriber.awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
    }

    @Test
    void getTokenAttachments() {
        final String filename = "filename";
        Token token = new Token();
        token.setContractFilename(filename);
        token.setName(filename);
        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.stream()).thenReturn(Multi.createFrom().item(token));

        PanacheMock.mock(Token.class);
        when(Token.find("onboardingId = ?1 and type = ?2", onboardingId, ATTACHMENT.name()))
                .thenReturn(queryPage);

        UniAssertSubscriber<List<String>> subscriber = tokenService.getAttachments(onboardingId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        List<String> actual = subscriber.awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertFalse(actual.isEmpty());
        Assertions.assertEquals(filename, actual.get(0));
    }
}
