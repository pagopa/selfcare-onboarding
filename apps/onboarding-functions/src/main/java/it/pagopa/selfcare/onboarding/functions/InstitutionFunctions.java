package it.pagopa.selfcare.onboarding.functions;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.durabletask.RetryPolicy;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import com.microsoft.durabletask.azurefunctions.DurableClientInput;
import com.microsoft.durabletask.azurefunctions.DurableOrchestrationTrigger;
import it.pagopa.selfcare.onboarding.config.RetryPolicyConfig;
import it.pagopa.selfcare.onboarding.dto.UserInstitutionFilters;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.*;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstitutionFunctions {
  private static final Logger logger = LoggerFactory.getLogger(InstitutionFunctions.class.getName());
  private static final String FORMAT_LOGGER_INSTITUTION_STRING = "%s: %s";
  private static final String CREATED_DELETE_INSTITUTION_ORCHESTRATION_WITH_INSTANCE_ID_MSG = "Created new DeleteInstitutionAndUser orchestration with instance ID = ";
  private final InstitutionService institutionService;
  private final UserService userService;
  private final OnboardingService onboardingService;
  private final ObjectMapper objectMapper;
  private final TaskOptions optionsRetry;

  public InstitutionFunctions(ObjectMapper objectMapper,
                              UserService userService,
                              InstitutionService institutionService,
                              OnboardingService onboardingService,
                              RetryPolicyConfig retryPolicyConfig) {
    this.objectMapper = objectMapper;
    this.institutionService = institutionService;
    this.userService = userService;
    this.onboardingService = onboardingService;
    final int maxAttempts = retryPolicyConfig.maxAttempts();
    final Duration firstRetryInterval = Duration.ofSeconds(retryPolicyConfig.firstRetryInterval());
    RetryPolicy retryPolicy = new RetryPolicy(maxAttempts, firstRetryInterval);
    retryPolicy.setBackoffCoefficient(retryPolicyConfig.backoffCoefficient());
    this.optionsRetry = new TaskOptions(retryPolicy);
  }

  /**
   * This HTTP-triggered function invokes an orchestration to set the status of institution and user to DELETED
   */
  @FunctionName("TriggerDeleteInstitutionAndUser")
  public HttpResponseMessage deleteInstitutionAndUserTrigger(
    @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
    @DurableClientInput(name = "durableContext") DurableClientContext durableContext,

    final ExecutionContext context) {
    context.getLogger().info("TriggerDeleteInstitutionAndUser processed a request");

    final String onboardingId = request.getQueryParameters().get("onboardingId");
    if (Objects.isNull(onboardingId) || StringUtils.isBlank(onboardingId)) {
      return request
              .createResponseBuilder(HttpStatus.BAD_REQUEST)
              .body("onboardingId can't be null or empty")
              .build();
    }
    DurableTaskClient client = durableContext.getClient();
    String instanceId = client.scheduleNewOrchestrationInstance("DeleteInstitutionAndUser", onboardingId);
    context.getLogger().info(() -> String.format("%s %s", CREATED_DELETE_INSTITUTION_ORCHESTRATION_WITH_INSTANCE_ID_MSG, instanceId));

    return durableContext.createCheckStatusResponse(request, instanceId);
  }

  /**
   * This function is the orchestrator that manages the deletion of user and institution
   */
  @FunctionName("DeleteInstitutionAndUser")
  public void deleteInstitutionAndUser(
    @DurableOrchestrationTrigger(name = "taskOrchestrationContext") TaskOrchestrationContext ctx,
    ExecutionContext functionContext) throws JsonProcessingException {

    String onboardingId = ctx.getInput(String.class);
    if (functionContext.getLogger().isLoggable(Level.INFO)) {
      functionContext.getLogger().info("DeleteInstitutionAndUser orchestration started with input: " + onboardingId);
    }

    Onboarding onboarding = getOnboarding(onboardingId);
    UserInstitutionFilters filters = getUserInstitutionFilters(onboarding);
    String filtersString = objectMapper.writeValueAsString(filters);

    ctx.callActivity(
                    DELETE_INSTITUTION_ACTIVITY_NAME,
                    filtersString,
                    optionsRetry,
                    String.class)
            .await();

    processUserDeletions(ctx, onboarding, filters);

    functionContext.getLogger().info("DeleteInstitutionAndUser orchestration completed");
  }

  /** This is the activity function that gets invoked by the orchestrator function. */
  @FunctionName(DELETE_INSTITUTION_ACTIVITY_NAME)
  public void deleteInstitution(
          @DurableActivityTrigger(name = "filtersString") String filtersString,
          final ExecutionContext context) throws JsonProcessingException {
    context
            .getLogger()
            .info(
                    () ->
                            String.format(
                                    FORMAT_LOGGER_INSTITUTION_STRING,
                                    DELETE_INSTITUTION_ACTIVITY_NAME,
                                    filtersString));
    UserInstitutionFilters filters = objectMapper.readValue(filtersString, UserInstitutionFilters.class);
    institutionService.deleteByIdAndProductId(filters.getInstitutionId(), filters.getProductId());
  }

  /** This is the activity function that gets invoked by the orchestrator function. */
  @FunctionName(DELETE_USER_ACTIVITY_NAME)
  public void deleteUser(
          @DurableActivityTrigger(name = "filtersString") String filtersString,
          final ExecutionContext context) throws JsonProcessingException {
    context
            .getLogger()
            .info(
                    () ->
                            String.format(
                                    FORMAT_LOGGER_INSTITUTION_STRING,
                                    DELETE_USER_ACTIVITY_NAME,
                                    filtersString));
    UserInstitutionFilters filters = objectMapper.readValue(filtersString, UserInstitutionFilters.class);
    userService.deleteByIdAndInstitutionIdAndProductId(filters.getUserId(), filters.getInstitutionId(), filters.getProductId());
  }

  private static UserInstitutionFilters getUserInstitutionFilters(Onboarding onboarding) {
    return UserInstitutionFilters
            .builder()
            .institutionId(onboarding.getInstitution().getId())
            .productId(onboarding.getProductId())
            .build();
  }

  private Onboarding getOnboarding(String onboardingId) {
    return onboardingService
            .getOnboarding(onboardingId)
            .orElseThrow(
                    () ->
                            new ResourceNotFoundException(
                                    String.format("Onboarding with id %s not found!", onboardingId)));
  }

  private void processUserDeletions(TaskOrchestrationContext ctx, Onboarding onboarding, UserInstitutionFilters filters) throws JsonProcessingException {

    logger.info("processUserDeletions started with filters: {}", filters);

    List<String> userIds = new java.util.ArrayList<>(onboarding.getUsers().stream().map(User::getId).toList());
    userIds.addAll(userService.findByInstitutionAndProduct(onboarding.getInstitution().getId(), onboarding.getProductId()));

    for (String userId : userIds) {
      filters.setUserId(userId);
      var enrichedFilters = objectMapper.writeValueAsString(filters);

      ctx.callActivity(
                      DELETE_USER_ACTIVITY_NAME,
                      enrichedFilters,
                      optionsRetry,
                      String.class)
              .await();
    }

    logger.debug("processUserDeletions completed");

  }

}
