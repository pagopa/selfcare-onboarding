package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.controller.response.*;
import it.pagopa.selfcare.onboarding.entity.*;
import jakarta.inject.Inject;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class InstitutionServiceDefaultTest {

    @Inject
    InstitutionServiceDefault institutionService;


    @Test
    void getInstitutions() {
        Onboarding onboarding = new Onboarding();
        onboarding.setInstitution(dummyInstitution());
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().item(onboarding));
        when(Onboarding.find(any())).thenReturn(query);
        final List<String> institutionIds = List.of("institutionId");

        AssertSubscriber<InstitutionResponse> subscriber = institutionService
                .getInstitutions(institutionIds)
                .subscribe().withSubscriber(AssertSubscriber.create(20));

        // Attendi il completamento e verifica i risultati
        subscriber.awaitCompletion().assertCompleted();
        List<InstitutionResponse> resultList = subscriber.getItems();

        // Verifica il risultato
        assertNotNull(resultList);
        assertFalse(resultList.isEmpty());
        resultList.forEach(actual -> {
            assertNotNull(actual);
            assertEquals(onboarding.getInstitution().getId(), actual.getId());
        });
    }

    @Test
    @RunOnVertxContext
    void getInstitutions_withEmptyIds(UniAsserter asserter) {
        final List<String> institutionIds = List.of();
        PanacheMock.mock(Onboarding.class);
        asserter.execute(() -> institutionService.getInstitutions(institutionIds));
        PanacheMock.verifyNoInteractions(Onboarding.class);
    }

    private Institution dummyInstitution() {
        Institution institution = new Institution();
        institution.setId("institutionId");
        institution.setInstitutionType(InstitutionType.GSP);
        return institution;
    }
}
