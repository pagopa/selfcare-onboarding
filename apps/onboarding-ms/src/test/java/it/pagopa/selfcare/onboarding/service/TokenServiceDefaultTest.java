package it.pagopa.selfcare.onboarding.service;

import static it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT;
import static it.pagopa.selfcare.onboarding.common.TokenType.INSTITUTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate;
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
import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import jakarta.inject.Inject;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        token.setType(TokenType.INSTITUTION);
        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.firstResult()).thenReturn(Uni.createFrom().item(token));

        PanacheMock.mock(Token.class);
        when(Token.find("onboardingId = ?1 and type = ?2", onboardingId, INSTITUTION.name()))
                .thenReturn(queryPage);

        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(new File("fileName"));

        UniAssertSubscriber<RestResponse<File>> subscriber = tokenService.retrieveContractNotSigned(onboardingId)
            .subscribe().withSubscriber(UniAssertSubscriber.create());

        RestResponse<File> actual = subscriber.awaitItem().getItem();
        assertNotNull(actual);
        assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
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
        assertNotNull(actual);
        assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
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
        assertNotNull(actual);
        Assertions.assertFalse(actual.isEmpty());
        assertEquals(filename, actual.get(0));
    }

    @Test
    void updateContractSignedTest_OK() {
        // given
        String onboardingId = "testContractSigned";
        String documentSignedPath = "/test/";
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("contractSigned", documentSignedPath);

        ReactivePanacheUpdate queryPage = mock(ReactivePanacheUpdate.class);
        PanacheMock.mock(Token.class);
        when(queryPage.where(anyString(), anyString())).thenReturn(Uni.createFrom().item(1L));
        when(Token.update(QueryUtils.buildUpdateDocument(testMap))).thenReturn(queryPage);

        // when
        UniAssertSubscriber<Long> result = tokenService.updateContractSigned(onboardingId, documentSignedPath).subscribe()
            .withSubscriber(UniAssertSubscriber.create());

        // then
        assertNotNull(result);
        assertEquals(1L, result.assertCompleted().awaitItem().getItem());

    }

    @Test
    void updateContractSignedTest_KO() {
        // given
        String onboardingId = "testContractSigned";
        String documentSignedPath = "/test/";

        Map<String, Object> testMap = new HashMap<>();
        testMap.put("contractSigned", documentSignedPath);

        ReactivePanacheUpdate queryPage = mock(ReactivePanacheUpdate.class);
        PanacheMock.mock(Token.class);
        when(queryPage.where(anyString(), anyString())).thenReturn(Uni.createFrom().item(Long.valueOf("0")));
        when(Token.update(QueryUtils.buildUpdateDocument(testMap))).thenReturn(queryPage);

        // when
        UniAssertSubscriber<Long> result = tokenService.updateContractSigned(onboardingId, documentSignedPath).subscribe()
            .withSubscriber(UniAssertSubscriber.create());

        // then
        assertNotNull(result);
        assertEquals("Token with id testContractSigned not found or already deleted", result.getFailure().getMessage());

    }


}
