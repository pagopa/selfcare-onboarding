package it.pagopa.selfcare.onboarding.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.controller.request.ContractRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingImportContract;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@FieldNameConstants(asEnum = true)
@MongoEntity(collection="onboardings")
public class Onboarding extends ReactivePanacheMongoEntity  {

    public ObjectId id;

    private String productId;
    private WorkflowType workflowType;
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
    private OnboardingStatus status;
    private String userRequestUid;
}
