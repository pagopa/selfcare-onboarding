package it.pagopa.selfcare.repository;

import com.mongodb.client.model.Filters;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.entity.Onboarding;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.BsonObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class OnboardingRepository {

    public static final String COLLECTION_NAME = "onboardings";
    @ConfigProperty(name = "quarkus.mongodb.database")
    String mongodbDatabase;

    @Inject
    ReactiveMongoClient mongoClient;


    public Uni<Onboarding> persistOrUpdate(Onboarding model) {
        return Objects.nonNull(model.getId())
                ? Uni.createFrom().item(model)
                    .onItem().invoke(onboarding -> onboarding.setUpdatedAt(LocalDateTime.now()))
                    .onItem().transformToUni(onboarding -> getCollection().findOneAndReplace(Filters.eq("_id", onboarding.getId()), onboarding))
                : Uni.createFrom().item(model)
                    .onItem().invoke(onboarding -> onboarding.setCreatedAt(LocalDateTime.now()))
                    .onItem().transformToUni(onboarding -> getCollection().insertOne(onboarding)
                        .onItem().transform(insertOneResult -> {
                            onboarding.setId(Optional.ofNullable(insertOneResult.getInsertedId())
                                            .map(id -> id.asObjectId().getValue())
                                            .orElse(null));
                            return onboarding;
                        }));
    }


    private ReactiveMongoCollection<Onboarding> getCollection(){
        return mongoClient
                .getDatabase(mongodbDatabase)
                .getCollection(COLLECTION_NAME, Onboarding.class);
    }
}
