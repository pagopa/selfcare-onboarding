package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.entity.Token;
import jakarta.inject.Inject;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class TokenServiceDefaultTest {

    @Inject
    TokenServiceDefault tokenService;
    @InjectMock
    AzureBlobClient azureBlobClient;

    @Test
    void getToken() {
        final String onboardingId = "onboardingId";
        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.list()).thenReturn(Uni.createFrom().item(List.of(new Token())));

        PanacheMock.mock(Token.class);
        when(Token.find("onboardingId", onboardingId))
                .thenReturn(queryPage);

        UniAssertSubscriber<List<Token>> subscriber = tokenService.getToken(onboardingId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();
    }

    @Test
    void retrieveContractNotSigned() {
        Token token = new Token();
        token.setContractFilename("fileName");
        final String onboardingId = "onboardingId";
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
}
