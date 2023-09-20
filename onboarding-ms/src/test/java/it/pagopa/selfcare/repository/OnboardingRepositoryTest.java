package it.pagopa.selfcare.repository;

import com.mongodb.client.result.InsertOneResult;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.quarkus.test.InjectMock;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.entity.Onboarding;
import jakarta.inject.Inject;
import org.bson.BsonObjectId;
import org.bson.BsonValue;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
public class OnboardingRepositoryTest {


    @Inject
    OnboardingRepository repository;

    @InjectMock
    ReactiveMongoClient mongoClient;


    ReactiveMongoCollection reactiveMongoCollection;

    Onboarding onboarding;

    @BeforeEach
    public void before() {
        ReactiveMongoDatabase reactiveMongoDatabase = Mockito.mock(ReactiveMongoDatabase.class);
        reactiveMongoCollection = Mockito.mock(ReactiveMongoCollection.class);
        Mockito.when(reactiveMongoDatabase.getCollection(any(), any())).thenReturn(reactiveMongoCollection);

        Mockito.when(mongoClient.getDatabase(any())).thenReturn(reactiveMongoDatabase);


        onboarding = new Onboarding();
    }


    @Test
    public void shouldUpdteIfContractHasObjectId() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(ObjectId.get());
        onboarding.setProductId("setProductId");

        Mockito.when(reactiveMongoCollection.findOneAndReplace(any(Bson.class), any()))
                .thenAnswer(I -> Uni.createFrom().item((Onboarding) I.getArguments()[1]));

        UniAssertSubscriber<Onboarding> subscriber = repository.persistOrUpdate(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem();

        Onboarding result = subscriber.assertCompleted().getItem();
        assertNotNull(result);
        assertEquals(result.getProductId(), onboarding.getProductId());
    }

    @Test
    public void shouldPersitIfContractNotHasObjectId() {
        Onboarding newContract = new Onboarding();
        ObjectId id = ObjectId.get();

        Mockito.when(reactiveMongoCollection.insertOne(any(Onboarding.class)))
                .thenAnswer(I -> Uni.createFrom().item(new InsertOneResult() {
                    @Override
                    public boolean wasAcknowledged() {
                        return false;
                    }

                    @Override
                    public BsonValue getInsertedId() {
                        return new BsonObjectId(id);
                    }
                }));

        UniAssertSubscriber<Onboarding> subscriber = repository.persistOrUpdate(newContract)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem();

        Onboarding result = subscriber.assertCompleted().getItem();
        assertNotNull(result);
        assertEquals(result.getId(), id);
    }
}
