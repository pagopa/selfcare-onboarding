package it.pagopa.selfcare.onboarding.steps;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import jakarta.inject.Inject;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

@QuarkusTest
public class IntegrationOperationUtils {

  @Inject OnboardingRepository onboardingRepository;

  @Inject TokenRepository tokenRepository;

  public <T> void persistIntoMongo(T input) {
    if (input instanceof Token) {
      tokenRepository.persist((Token) input);
    } else {
      onboardingRepository.persist((Onboarding) input);
    }
  }

  public Onboarding findIntoMongoOnboarding(String id) {
    return onboardingRepository.findById(id);
  }

  public Token findIntoMongoToken(String id) {
    return tokenRepository.findById(id);
  }

  public void persistInstitution(MongoDatabase mongoDatabase, Institution institution) {
    CodecRegistry pojoCodecRegistry =
        CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    MongoCollection<Institution> collection =
        mongoDatabase.getCollection("Institution", Institution.class).withCodecRegistry(pojoCodecRegistry);

    collection.insertOne(institution);
  }
}
