package it.pagopa.selfcare.onboarding.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import it.pagopa.selfcare.onboarding.common.TokenType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection="tokens")
public class Token extends ReactivePanacheMongoEntity {

    private ObjectId id;
    private TokenType type;
    private String onboardingId;
    private String productId;
    private String checksum;
    private String contractVersion;
    private String contractTemplate;
    private String contractSigned;
    //@Indexed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime activatedAt;

}

