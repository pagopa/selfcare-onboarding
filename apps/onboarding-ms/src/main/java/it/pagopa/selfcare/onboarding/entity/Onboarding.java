package it.pagopa.selfcare.onboarding.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.bson.codecs.pojo.annotations.BsonId;

@EqualsAndHashCode(callSuper = true)
@Data
@FieldNameConstants(asEnum = true)
@MongoEntity(collection="onboardings")
public class Onboarding extends ReactivePanacheMongoEntityBase {

    @BsonId
    public String id;

    private String productId;
    private List<String> testEnvProductIds;
    private WorkflowType workflowType;
    private Institution institution;
    private List<User> users;
    private List<AggregateInstitution> aggregates;
    private String pricingPlan;
    private Billing billing;
    private Boolean signContract;

    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiringDate;
    private OnboardingStatus status;
    private String userRequestUid;
    private AdditionalInformations additionalInformations;
    private String reasonForReject;
    private Boolean isAggregator;

    private String referenceOnboardingId;
    private String previousManagerId;
}
