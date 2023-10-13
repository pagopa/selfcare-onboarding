package it.pagopa.selfcare.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import it.pagopa.selfcare.controller.request.ContractRequest;
import it.pagopa.selfcare.controller.request.OnboardingImportContract;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection="onboardings")
public class Onboarding extends ReactivePanacheMongoEntity  {

    public ObjectId id;

    private String productId;
    private Institution institution;
    private List<User> users;
    private String pricingPlan;
    private Billing billing;
    private ContractRequest contract;
    private OnboardingImportContract contractImported;
    private Boolean signContract;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiringDate;
}
