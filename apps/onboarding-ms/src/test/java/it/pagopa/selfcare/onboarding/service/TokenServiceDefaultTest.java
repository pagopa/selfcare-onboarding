package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.entity.Token;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class TokenServiceDefaultTest {

    @Inject
    TokenServiceDefault tokenService;

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
}
