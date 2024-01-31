package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class TokenServiceDefaultTest {

    @InjectMock
    TokenServiceDefault tokenService;

    @Test
    void getToken() {
        final String onboardingId = "onboardingId";
        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.stream()).thenReturn(Multi.createFrom().items(new Token()));

        PanacheMock.mock(Token.class);
        when(Token.find("onboardingId", onboardingId)).thenReturn(queryPage);

        tokenService.getToken(onboardingId)
                .subscribe().withSubscriber(AssertSubscriber.create())
                .assertCompleted();
    }
}
