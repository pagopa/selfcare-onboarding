package it.pagopa.selfcare.repository;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import it.pagopa.selfcare.entity.Onboarding;
import jakarta.inject.Inject;

public class OnboardingRepository {


    @ConfigProperty(name = "quarkus.mongodb.database")
    String mongodbDatabase;

    @Inject
    ReactiveMongoClient mongoClient;


    private ReactiveMongoCollection<Onboarding> getCollection(){
        return mongoClient
                .getDatabase(mongodbDatabase)
                .getCollection("contracts", Contract.class);
    }
}
