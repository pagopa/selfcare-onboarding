package it.pagopa.selfcare.onboarding.functions;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskFailedException;
import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.AggregateInstitution;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.CompletionService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.onboarding.utils.Utils;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.openapi.quarkus.core_json.model.DelegationResponse;

/** Unit test for Function class. */
@QuarkusTest
class OnboardingFunctionsTest {

  @Inject OnboardingFunctions function;

  @InjectMock OnboardingService service;

  @InjectMock CompletionService completionService;

  @InjectMock ProductService productService;

  @Inject ObjectMapper objectMapper;

  final String onboardingStringBase = "{\"onboardingId\":\"onboardingId\"}";

  final String onboardingWorkflowString =
          "{\"type\":\"INSTITUTION\",\"onboardingString\":{\"id\":\"id\",\"productId\":\"prod-test\",\"testEnvProductIds\":null,\"workflowType\":\"FOR_APPROVE\",\"institution\":null,\"users\":null,\"aggregates\":null,\"pricingPlan\":null,\"billing\":null,\"signContract\":null,\"expiringDate\":null,\"status\":\"REQUEST\",\"userRequestUid\":null,\"workflowInstanceId\":null,\"createdAt\":null,\"updatedAt\":null,\"activatedAt\":null,\"deletedAt\":null,\"reasonForReject\":null,\"isAggregator\":null}}";

  final String onboardingString =
          "{\"id\":\"id\",\"productId\":\"prod-test\",\"testEnvProductIds\":null,\"workflowType\":\"FOR_APPROVE\",\"institution\":null,\"users\":null,\"aggregates\":null,\"pricingPlan\":null,\"billing\":null,\"signContract\":null,\"expiringDate\":null,\"status\":\"REQUEST\",\"userRequestUid\":null,\"workflowInstanceId\":null,\"createdAt\":null,\"updatedAt\":null,\"activatedAt\":null,\"deletedAt\":null,\"reasonForReject\":null,\"isAggregator\":null}";

  final String onboardingString2 =
          "{\"id\":\"id\",\"productId\":\"prod-test\",\"testEnvProductIds\":null,\"workflowType\":\"CONTRACT_REGISTRATION\",\"institution\":null,\"users\":null,\"aggregates\":null,\"pricingPlan\":null,\"billing\":null,\"signContract\":null,\"expiringDate\":null,\"status\":\"REQUEST\",\"userRequestUid\":null,\"workflowInstanceId\":null,\"createdAt\":null,\"updatedAt\":null,\"activatedAt\":null,\"deletedAt\":null,\"reasonForReject\":null,\"isAggregator\":null}";

  final String onboardingAttachmentString =
          "{\"onboardingString\":{\"id\":\"id\",\"productId\":\"prod-test\",\"testEnvProductIds\":null,\"workflowType\":\"FOR_APPROVE\",\"institution\":null,\"users\":null,\"aggregates\":null,\"pricingPlan\":null,\"billing\":null,\"signContract\":null,\"expiringDate\":null,\"status\":\"REQUEST\",\"userRequestUid\":null,\"workflowInstanceId\":null,\"createdAt\":null,\"updatedAt\":null,\"activatedAt\":null,\"deletedAt\":null,\"reasonForReject\":null,\"isAggregator\":null},\"attachmentTemplate\":{"
                  + "\"templatePath\": null, \"templateVersion\": null, \"name\": null, \"mandatory\": null, \"generated\": null, \"workflowType\": null, \"workflowState\": null, \"order\": null}}";

  final String onboardingWithoutInstitutionIdString =
          "{\"id\":\"id\",\"productId\":\"prod-test\",\"testEnvProductIds\":null,\"workflowType\":\"FOR_APPROVE\",\"institution\":{\"id\":null},\"users\":null,\"aggregates\":null,\"pricingPlan\":null,\"billing\":null,\"signContract\":null,\"expiringDate\":null,\"status\":\"REQUEST\",\"userRequestUid\":null,\"workflowInstanceId\":null,\"createdAt\":null,\"updatedAt\":null,\"activatedAt\":null,\"deletedAt\":null,\"reasonForReject\":null,\"isAggregator\":null}";

  final String onboardingWithInstitutionIdString =
          "{\"id\":\"id\",\"productId\":\"prod-test\",\"testEnvProductIds\":null,\"workflowType\":\"FOR_APPROVE\",\"institution\":{\"id\":\"inst123\"},\"users\":null,\"aggregates\":null,\"pricingPlan\":null,\"billing\":null,\"signContract\":null,\"expiringDate\":null,\"status\":\"REQUEST\",\"userRequestUid\":null,\"workflowInstanceId\":null,\"createdAt\":null,\"updatedAt\":null,\"activatedAt\":null,\"deletedAt\":null,\"reasonForReject\":null,\"isAggregator\":null}";

  static ExecutionContext executionContext;

  static {
    executionContext = mock(ExecutionContext.class);
    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
  }

  @Test
  void startAndWaitOrchestration_failedOrchestration() throws Exception {
    @SuppressWarnings("unchecked")
    final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

    final Map<String, String> queryParams = new HashMap<>();
    final String onboardingId = "onboardingId";
    queryParams.put("onboardingId", onboardingId);
    queryParams.put("timeout", "10");
    doReturn(queryParams).when(req).getQueryParameters();

    final Optional<String> queryBody = Optional.empty();
    doReturn(queryBody).when(req).getBody();

    doAnswer(
            (Answer<HttpResponseMessage.Builder>)
                    invocation -> {
                      HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                      return new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
                              .status(status);
                    })
            .when(req)
            .createResponseBuilder(any(HttpStatus.class));

    final ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();

    final DurableClientContext durableContext = mock(DurableClientContext.class);
    final DurableTaskClient client = mock(DurableTaskClient.class);
    final String scheduleNewOrchestrationInstance = "scheduleNewOrchestrationInstance";
    doReturn(client).when(durableContext).getClient();
    doReturn(scheduleNewOrchestrationInstance)
            .when(client)
            .scheduleNewOrchestrationInstance("Onboardings", onboardingId);

    HttpResponseMessage responseMessage = function.startOrchestration(req, durableContext, context);

    verify(client, times(1)).waitForInstanceCompletion(anyString(), any(), anyBoolean());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseMessage.getStatusCode());
  }

  @Test
  void onboardingsOrchestrator_throwExceptionIfOnboardingNotPresent() {
    final String onboardingId = "onboardingId";
    TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);

    when(orchestrationContext.getInput(String.class)).thenReturn(onboardingId);
    when(service.getOnboarding(onboardingId)).thenReturn(Optional.empty());
    assertThrows(
            ResourceNotFoundException.class,
            () -> function.onboardingsOrchestrator(orchestrationContext, executionContext));

    verify(service, times(1))
            .updateOnboardingStatusAndInstanceId(
                    onboardingId, OnboardingStatus.FAILED, orchestrationContext.getInstanceId());
  }

  @Test
  void onboardingsOrchestratorContractRegistration() {
    Onboarding onboarding = new Onboarding();
    List<User> users = new ArrayList<>();
    User user = new User();
    users.add(user);
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.REQUEST);
    onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);
    onboarding.setUsers(users);
    onboarding.setInstitution(new Institution());

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(4))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(0));
    assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
    assertEquals(SEND_MAIL_REGISTRATION_FOR_CONTRACT, captorActivity.getAllValues().get(2));
    assertEquals(SEND_MAIL_REGISTRATION_FOR_USER, captorActivity.getAllValues().get(3));

    verify(service, times(1)).updateOnboardingStatus(onboarding.getId(), OnboardingStatus.PENDING);
  }

  @Test
  void onboardingsOrchestratorContractRegistration_PRVMerchant() {
    Onboarding onboarding = new Onboarding();
    List<User> users = new ArrayList<>();
    User user = new User();
    users.add(user);
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.REQUEST);
    onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);
    onboarding.setUsers(users);
    Institution institution = new Institution();
    institution.setTaxCode("taxCode");
    institution.setAtecoCodes(List.of("21.1.1"));
    onboarding.setInstitution(institution);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(5))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(0));
    assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
    assertEquals(SEND_MAIL_REGISTRATION_FOR_CONTRACT, captorActivity.getAllValues().get(2));
    assertEquals(SAVE_VISURA_FOR_MERCHANT, captorActivity.getAllValues().get(3));
    assertEquals(SEND_MAIL_REGISTRATION_FOR_USER, captorActivity.getAllValues().get(4));

    verify(service, times(1)).updateOnboardingStatus(onboarding.getId(), OnboardingStatus.PENDING);
  }

  @Test
  void onboardingOrchestratorContractRegistrationAggregator() {
    Onboarding onboarding = new Onboarding();
    List<User> users = new ArrayList<>();
    User user = new User();
    users.add(user);
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.REQUEST);
    onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION_AGGREGATOR);
    onboarding.setUsers(users);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(5))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_AGGREGATES_CSV_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
    assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(2));
    assertEquals(SEND_MAIL_REGISTRATION_FOR_CONTRACT, captorActivity.getAllValues().get(3));
    assertEquals(SEND_MAIL_REGISTRATION_FOR_USER, captorActivity.getAllValues().get(4));


    verify(service, times(1)).updateOnboardingStatus(onboarding.getId(), OnboardingStatus.PENDING);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);
  }

  @Test
  void onboardingOrchestratorContractRegistrationAggregator_Pending() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    AggregateInstitution aggregate1 = new AggregateInstitution();
    aggregate1.setTaxCode("code1");
    AggregateInstitution aggregate2 = new AggregateInstitution();
    aggregate1.setTaxCode("code2");
    AggregateInstitution aggregate3 = new AggregateInstitution();
    aggregate1.setTaxCode("code3");
    onboarding.setAggregates(List.of(aggregate1, aggregate2, aggregate3));
    Institution institution = new Institution();
    institution.setId("id");
    onboarding.setInstitution(institution);
    onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION_AGGREGATOR);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
    Task<String> verifyTask = mockTaskWithValue(onboardingWithoutInstitutionIdString);
    when(orchestrationContext.callActivity(eq(VERIFY_ONBOARDING_AGGREGATE_ACTIVITY), any(), eq(String.class)))
            .thenReturn(verifyTask);
    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    Mockito.verify(orchestrationContext, times(5))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
    assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
    assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(4));

    Mockito.verify(orchestrationContext, times(3))
            .callSubOrchestrator(eq(ONBOARDINGS_AGGREGATE_ORCHESTRATOR), any(), any());

    Mockito.verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);
  }

  @Test
  void onboardingOrchestratorContractRegistrationAggregatorWithExistsAggregateOnboarding_Pending() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    AggregateInstitution aggregate1 = new AggregateInstitution();
    aggregate1.setTaxCode("code1");
    AggregateInstitution aggregate2 = new AggregateInstitution();
    aggregate1.setTaxCode("code2");
    AggregateInstitution aggregate3 = new AggregateInstitution();
    aggregate1.setTaxCode("code3");
    onboarding.setAggregates(List.of(aggregate1, aggregate2, aggregate3));
    Institution institution = new Institution();
    institution.setId("id");
    onboarding.setInstitution(institution);
    onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION_AGGREGATOR);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
    Task<String> verifyTask2 = mockTaskWithValue("false");
    Task<String> verifyWithInstitution = mockTaskWithValue(onboardingWithInstitutionIdString);
    Task<String> verifyWithoutInstitution = mockTaskWithValue(onboardingWithoutInstitutionIdString);

    when(orchestrationContext.callActivity(eq(VERIFY_ONBOARDING_AGGREGATE_ACTIVITY), any(), eq(String.class)))
            .thenReturn(verifyWithInstitution)  // 1
            .thenReturn(verifyWithInstitution)  // 2
            .thenReturn(verifyWithoutInstitution);
    when(orchestrationContext.callActivity(eq(EXISTS_DELEGATION_ACTIVITY), any(), eq(String.class)))
            .thenReturn(verifyTask2);
    Task<String> createDelegationTask = mockTaskWithValue("delegation-created");
    when(orchestrationContext.callActivity(eq(CREATE_DELEGATION_ACTIVITY), any(), eq(String.class)))
            .thenReturn(createDelegationTask);
    Task<String> createUserTask = mockTaskWithValue("user-created");
    when(orchestrationContext.callActivity(eq(CREATE_USERS_ACTIVITY), any(), eq(String.class)))
            .thenReturn(createUserTask);
    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    Mockito.verify(orchestrationContext, times(5))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
    assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
    assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(4));

    Mockito.verify(orchestrationContext, times(1))
            .callSubOrchestrator(eq(ONBOARDINGS_AGGREGATE_ORCHESTRATOR), any(), any());

    Mockito.verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);
  }

  @Test
  void onboardingOrchestratorIncrementRegistrationAggregator_Pending_delgationAlreadyExists() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    AggregateInstitution aggregate1 = new AggregateInstitution();
    aggregate1.setTaxCode("code1");
    AggregateInstitution aggregate2 = new AggregateInstitution();
    aggregate2.setTaxCode("code2");
    onboarding.setAggregates(List.of(aggregate1, aggregate2));
    Institution institution = new Institution();
    institution.setId("id");
    onboarding.setInstitution(institution);
    onboarding.setWorkflowType(WorkflowType.INCREMENT_REGISTRATION_AGGREGATOR);

    TaskOrchestrationContext orchestrationContext =
            mockTaskOrchestrationContextForIncrementAggregator(onboarding, "true");
    Task<String> verifyTask = mockTaskWithValue(onboardingWithoutInstitutionIdString);
    when(orchestrationContext.callActivity(eq(VERIFY_ONBOARDING_AGGREGATE_ACTIVITY), any(), eq(String.class)))
            .thenReturn(verifyTask);
    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    Mockito.verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
  }

  @Test
  void onboardingOrchestratorIncrementRegistrationAggregator_Pending_delgationNotExists() {
    Onboarding onboarding = getOnboarding();
    TaskOrchestrationContext orchestrationContext =
            mockTaskOrchestrationContextForIncrementAggregator(onboarding, "false");
    Task<String> verifyTask = mockTaskWithValue(onboardingWithoutInstitutionIdString);
    when(orchestrationContext.callActivity(eq(VERIFY_ONBOARDING_AGGREGATE_ACTIVITY), any(), eq(String.class)))
            .thenReturn(verifyTask);
    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    Mockito.verify(orchestrationContext, times(2))
            .callSubOrchestrator(eq(ONBOARDINGS_AGGREGATE_ORCHESTRATOR), any(), any());

    Mockito.verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
  }

  @Test
  void onboardingOrchestratorConfirmationAggregator() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.REQUEST);
    onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATOR);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(3))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_AGGREGATES_CSV_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
    assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(2));

    verify(service, times(1)).updateOnboardingStatus(onboarding.getId(), OnboardingStatus.PENDING);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);
  }

  @Test
  void onboardingOrchestratorConfirmationAggregator_Pending() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    AggregateInstitution aggregate1 = new AggregateInstitution();
    aggregate1.setTaxCode("code1");
    AggregateInstitution aggregate2 = new AggregateInstitution();
    aggregate1.setTaxCode("code2");
    AggregateInstitution aggregate3 = new AggregateInstitution();
    aggregate1.setTaxCode("code3");
    onboarding.setAggregates(List.of(aggregate1, aggregate2, aggregate3));
    Institution institution = new Institution();
    institution.setId("id");
    onboarding.setInstitution(institution);
    onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATOR);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
    Task<String> verifyTask = mockTaskWithValue(onboardingWithoutInstitutionIdString);
    when(orchestrationContext.callActivity(eq(VERIFY_ONBOARDING_AGGREGATE_ACTIVITY), any(), eq(String.class)))
            .thenReturn(verifyTask);
    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    Mockito.verify(orchestrationContext, times(5))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
    assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
    assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(4));

    Mockito.verify(orchestrationContext, times(3))
            .callSubOrchestrator(eq(ONBOARDINGS_AGGREGATE_ORCHESTRATOR), any(), any());

    Mockito.verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);
  }

  private static Onboarding getOnboarding() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    AggregateInstitution aggregate1 = new AggregateInstitution();
    aggregate1.setTaxCode("code1");
    AggregateInstitution aggregate2 = new AggregateInstitution();
    aggregate2.setTaxCode("code2");
    onboarding.setAggregates(List.of(aggregate1, aggregate2));
    Institution institution = new Institution();
    institution.setId("id");
    onboarding.setInstitution(institution);
    onboarding.setWorkflowType(WorkflowType.INCREMENT_REGISTRATION_AGGREGATOR);
    return onboarding;
  }

  @Test
  void onboardingsOrchestratorNewAdminRequest() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.REQUEST);
    onboarding.setWorkflowType(WorkflowType.USERS);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(3))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(0));
    assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
    assertEquals(SEND_MAIL_REGISTRATION_FOR_CONTRACT, captorActivity.getAllValues().get(2));

    verify(service, times(1)).updateOnboardingStatus(onboarding.getId(), OnboardingStatus.PENDING);
  }

  @Test
  void onboardingsOrchestratorForApprove() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.REQUEST);
    onboarding.setWorkflowType(WorkflowType.FOR_APPROVE);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(1))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY, captorActivity.getAllValues().get(0));

    verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.TOBEVALIDATED);
  }

  @Test
  void onboardingsOrchestratorForApproveWhenToBeValidated() {
    Onboarding onboarding = new Onboarding();
    List<User> users = new ArrayList<>();
    User user = new User();
    users.add(user);
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);
    onboarding.setWorkflowType(WorkflowType.FOR_APPROVE);
    onboarding.setUsers(users);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(4))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(0));
    assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
    assertEquals(
            SEND_MAIL_REGISTRATION_FOR_CONTRACT_WHEN_APPROVE_ACTIVITY,
            captorActivity.getAllValues().get(2));
    assertEquals(SEND_MAIL_REGISTRATION_FOR_USER, captorActivity.getAllValues().get(3));

    verify(service, times(1)).updateOnboardingStatus(onboarding.getId(), OnboardingStatus.PENDING);
  }

  @Test
  void onboardingsOrchestratorConfirmation() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setWorkflowType(WorkflowType.CONFIRMATION);
    onboarding.setInstitution(new Institution());

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(5))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
    assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
    assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(4));

    verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
  }

  @Test
  void onboardingsOrchestratorConfirmationWithTestProductIds() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setWorkflowType(WorkflowType.CONFIRMATION);
    onboarding.setInstitution(new Institution());
    onboarding.setTestEnvProductIds(List.of("test1"));

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(7))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
    assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
    assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(4));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(5));
    assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(6));

    verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
  }

  @Test
  void onboardingOrchestratorConfirmAggregate() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATE);
    Institution institution = new Institution();
    institution.setId("id");
    onboarding.setInstitution(institution);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    Mockito.verify(orchestrationContext, times(5))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
    assertEquals(CREATE_DELEGATION_ACTIVITY, captorActivity.getAllValues().get(2));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(3));
    assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(4));

    Mockito.verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
    Mockito.verify(completionService, times(0)).sendCompletedEmailAggregate(any());

    function.onboardingsOrchestrator(orchestrationContext, executionContext);
  }

  @Test
  void onboardingsAggregateOrchestrator() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATE);
    onboarding.setInstitution(new Institution());
    onboarding.setTestEnvProductIds(List.of("test1"));

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
    Task<Object> mockTask = Mockito.mock(Task.class);
    when(orchestrationContext.callSubOrchestrator(any(), any(), any())).thenReturn(mockTask);

    function.onboardingsAggregateOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(2))
            .callActivity(captorActivity.capture(), any(), any(), any());
    verify(orchestrationContext, times(1)).callSubOrchestrator(eq("Onboardings"), any(), any());
    assertEquals(EXISTS_DELEGATION_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(
            CREATE_AGGREGATE_ONBOARDING_REQUEST_ACTIVITY, captorActivity.getAllValues().get(1));
  }

  @Test
  void onboardingsAggregateOrchestrator_resourceNotFound() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATE);
    onboarding.setInstitution(new Institution());
    onboarding.setTestEnvProductIds(List.of("test1"));

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    when(orchestrationContext.callActivity(any(), any(), any(), any()))
            .thenThrow(ResourceNotFoundException.class);

    assertThrows(
            ResourceNotFoundException.class,
            () -> function.onboardingsAggregateOrchestrator(orchestrationContext, executionContext));

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(1))
            .callActivity(captorActivity.capture(), any(), any(), any());

    assertEquals(EXISTS_DELEGATION_ACTIVITY, captorActivity.getAllValues().get(0));
    verify(service, times(1))
            .updateOnboardingStatusAndInstanceId(
                    null, OnboardingStatus.FAILED, orchestrationContext.getInstanceId());
  }

  @Test
  void onboardingsAggregateOrchestrator_taskFailed() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATE);
    onboarding.setInstitution(new Institution());
    onboarding.setTestEnvProductIds(List.of("test1"));

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    when(orchestrationContext.callActivity(any(), any(), any(), any()))
            .thenThrow(TaskFailedException.class);

    assertThrows(
            TaskFailedException.class,
            () -> function.onboardingsAggregateOrchestrator(orchestrationContext, executionContext));

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(1))
            .callActivity(captorActivity.capture(), any(), any(), any());

    assertEquals(EXISTS_DELEGATION_ACTIVITY, captorActivity.getAllValues().get(0));
    verify(service, times(1))
            .updateOnboardingStatusAndInstanceId(
                    null, OnboardingStatus.FAILED, orchestrationContext.getInstanceId());
  }

  @Test
  void onboardingsOrchestratorImport() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setWorkflowType(WorkflowType.IMPORT);
    onboarding.setInstitution(new Institution());

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(3))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));

    verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
  }

  @Test
  void onboardingsOrchestratorImportWithSendCompletionMail() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setWorkflowType(WorkflowType.IMPORT);
    onboarding.setSendMailForImport(Boolean.TRUE);
    onboarding.setInstitution(new Institution());

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(4))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
    assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(3));

    verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
  }

  @Test
  void onboardingsOrchestratorNewAdmin() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setWorkflowType(WorkflowType.USERS);
    onboarding.setInstitution(new Institution());

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(3))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(1));
    assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(2));

    verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
  }

  @Test
  void onboardingsOrchestratorRegistrationRequestApprove() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.REQUEST);
    onboarding.setWorkflowType(WorkflowType.FOR_APPROVE_PT);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(2))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY, captorActivity.getAllValues().get(1));

    verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.TOBEVALIDATED);
  }

  @Test
  void onboardingsOrchestratorForApprovePtWhenToBeValidated() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setInstitution(new Institution());
    onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);
    onboarding.setWorkflowType(WorkflowType.FOR_APPROVE_PT);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(5))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
    assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
    assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(4));

    verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
  }

  TaskOrchestrationContext mockTaskOrchestrationContext(Onboarding onboarding) {
    TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
    when(orchestrationContext.getInput(String.class)).thenReturn(onboarding.getId());
    when(service.getOnboarding(anyString())).thenReturn(Optional.of(onboarding));
    when(completionService.existsDelegation(any())).thenReturn("false");

    Task task = mock(Task.class);
    when(orchestrationContext.callActivity(any(), any(), any(), any())).thenReturn(task);

    when(orchestrationContext.callSubOrchestrator(any(), any())).thenReturn(task);
    when(task.await()).thenReturn("false");
    when(orchestrationContext.allOf(anyList())).thenReturn(task);
    return orchestrationContext;
  }

  TaskOrchestrationContext mockTaskOrchestrationContextForIncrementAggregator(
          Onboarding onboarding, String returnValue) {
    TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
    when(orchestrationContext.getInput(String.class)).thenReturn(onboarding.getId());
    when(service.getOnboarding(onboarding.getId())).thenReturn(Optional.of(onboarding));
    when(completionService.existsDelegation(any())).thenReturn("true");

    Task task = mock(Task.class);
    when(orchestrationContext.callActivity(any(), any(), any(), any())).thenReturn(task);
    when(orchestrationContext.callSubOrchestrator(any(), any())).thenReturn(task);
    when(task.await()).thenReturn(returnValue);
    when(orchestrationContext.allOf(anyList())).thenReturn(task);
    return orchestrationContext;
  }

  TaskOrchestrationContext mockTaskOrchestrationContextForUsersEa(
          Onboarding onboarding, List<DelegationResponse> delegationResponseList) {
    TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
    when(orchestrationContext.getInput(String.class)).thenReturn(onboarding.getId());
    when(service.getOnboarding(anyString())).thenReturn(Optional.of(onboarding));
    when(completionService.retrieveAggregates(any())).thenReturn(delegationResponseList);
    String delegationResponseListString =
            Utils.getDelegationResponseListString(objectMapper, delegationResponseList);

    Task task = mock(Task.class);
    when(orchestrationContext.callActivity(any(), any(), any(), any())).thenReturn(task);
    when(task.await()).thenReturn(delegationResponseListString);
    when(orchestrationContext.allOf(anyList())).thenReturn(task);
    return orchestrationContext;
  }

  @Test
  void buildContract() {

    doNothing().when(service).createContract(any());

    function.buildContract(onboardingWorkflowString, executionContext);

    verify(service, times(1)).createContract(any());
  }

  @Test
  void buildAttachmentsAndSaveTokens_validBody_returnsAccepted() {
    // Mock HttpRequestMessage with valid body
    final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
    doReturn(Optional.of(onboardingString)).when(req).getBody();

    doAnswer(
            (Answer<HttpResponseMessage.Builder>)
                    invocation -> {
                      HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                      return new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
                              .status(status);
                    })
            .when(req)
            .createResponseBuilder(any(HttpStatus.class));

    final ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();

    final DurableClientContext durableContext = mock(DurableClientContext.class);
    final DurableTaskClient client = mock(DurableTaskClient.class);
    final String instanceId = "instanceId123";

    doReturn(client).when(durableContext).getClient();
    doReturn(instanceId)
            .when(client)
            .scheduleNewOrchestrationInstance("BuildAttachmentAndSaveToken", onboardingString);
    when(durableContext.createCheckStatusResponse(req, instanceId))
            .thenReturn(
                    new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
                            .status(HttpStatus.ACCEPTED)
                            .build());

    // Invoke
    HttpResponseMessage responseMessage =
            function.buildAttachmentsAndSaveTokens(req, durableContext, context);

    // Verify
    assertEquals(HttpStatus.ACCEPTED.value(), responseMessage.getStatusCode());
  }

  @Test
  void buildAttachmentsAndSaveTokens_emptyBody_returnsBadRequest() {
    // Mock HttpRequestMessage with empty body
    final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
    doReturn(Optional.empty()).when(req).getBody();

    doAnswer(
            (Answer<HttpResponseMessage.Builder>)
                    invocation -> {
                      HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                      return new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
                              .status(status);
                    })
            .when(req)
            .createResponseBuilder(any(HttpStatus.class));

    final ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();

    final DurableClientContext durableContext = mock(DurableClientContext.class);

    // Invoke
    HttpResponseMessage responseMessage =
            function.buildAttachmentsAndSaveTokens(req, durableContext, context);

    // Verify
    assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());
    assertEquals("Body can not be empty", responseMessage.getBody());
  }

  @Test
  void buildAttachmentAndSaveToken_invokeActivity() throws JsonProcessingException {
    // given
    Product product = createDummyProduct();
    when(productService.getProductIsValid(anyString())).thenReturn(product);

    TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
    when(orchestrationContext.getInput(String.class)).thenReturn(onboardingString);

    Task task = mock(Task.class);
    when(orchestrationContext.callActivity(any(), any(), any(), any())).thenReturn(task);
    when(task.await()).thenReturn("false");
    when(orchestrationContext.allOf(anyList())).thenReturn(task);

    // when
    function.buildAttachmentAndSaveToken(orchestrationContext, executionContext);

    // then
    verify(productService, times(1)).getProductIsValid(anyString());
    Mockito.verify(orchestrationContext, times(2)).callActivity(any(), any(), any(), any());
  }

  @Test
  void buildAttachmentAndSaveToken_noAttachments_invokeActivity() throws JsonProcessingException {
    // given
    Product product = createDummyProduct();

    when(productService.getProductIsValid(anyString())).thenReturn(product);

    TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
    when(orchestrationContext.getInput(String.class)).thenReturn(onboardingString2);

    Task task = mock(Task.class);
    when(orchestrationContext.callActivity(any(), any(), any(), any())).thenReturn(task);
    when(task.await()).thenReturn("false");
    when(orchestrationContext.allOf(anyList())).thenReturn(task);

    // when
    function.buildAttachmentAndSaveToken(orchestrationContext, executionContext);

    // then
    verify(productService, times(1)).getProductIsValid(anyString());
  }

  @Test
  void buildAttachment() {

    doNothing().when(service).createAttachment(any());

    function.buildAttachment(onboardingAttachmentString, executionContext);

    verify(service, times(1)).createAttachment(any());
  }

  @Test
  void saveTokenAttachment() {

    doNothing().when(service).saveTokenWithAttachment(any());

    function.saveTokenAttachment(onboardingAttachmentString, executionContext);

    verify(service, times(1)).saveTokenWithAttachment(any());
  }

  @Test
  void saveToken() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(service).saveTokenWithContract(any());

    function.saveToken(onboardingWorkflowString, executionContext);

    verify(service, times(1)).saveTokenWithContract(any());
  }

  @Test
  void sendMailRegistrationWithContract() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(service).sendMailRegistrationForContract(any());

    function.sendMailRegistrationForContract(onboardingWorkflowString, executionContext);

    verify(service, times(1)).sendMailRegistrationForContract(any());
  }

  @Test
  void sendMailRegistration() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(service).sendMailRegistration(any());

    function.sendMailRegistration(onboardingStringBase, executionContext);

    verify(service, times(1)).sendMailRegistration(any());
  }

  @Test
  void sendMailRegistrationForUser() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(service).sendMailRegistrationForUser(any());

    function.sendMailRegistrationForUser(onboardingStringBase, executionContext);

    verify(service, times(1)).sendMailRegistrationForUser(any());
  }

  @Test
  void saveVisuraForMerchant() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(service).saveVisuraForMerchant(any());

    function.saveVisuraForMerchant(onboardingStringBase, executionContext);

    verify(service, times(1)).saveVisuraForMerchant(any());
  }

  @Test
  void sendMailRegistrationApprove() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(service).sendMailRegistrationApprove(any());

    function.sendMailRegistrationApprove(onboardingStringBase, executionContext);

    verify(service, times(1)).sendMailRegistrationApprove(any());
  }

  @Test
  void sendMailOnboardingApprove() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(service).sendMailOnboardingApprove(any());

    function.sendMailOnboardingApprove(onboardingStringBase, executionContext);

    verify(service, times(1)).sendMailOnboardingApprove(any());
  }

  @Test
  void sendMailRegistrationWithContractWhenApprove() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(service).sendMailRegistrationForContractWhenApprove(any());

    function.sendMailRegistrationForContractWhenApprove(onboardingWorkflowString, executionContext);

    verify(service, times(1)).sendMailRegistrationForContractWhenApprove(any());
  }

  @Test
  void onboardingCompletionOrchestrator() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);
    onboarding.setInstitution(new Institution());

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(5))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
    assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
    assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(4));

    verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
  }

  @Test
  void onboardingRejectedOrchestrator() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.REJECTED);
    onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);
    onboarding.setInstitution(new Institution());

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(1))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(SEND_MAIL_REJECTION_ACTIVITY, captorActivity.getAllValues().get(0));
  }

  @Test
  void usersPgOrchestrator_whenStatusPending() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setWorkflowType(WorkflowType.USERS_PG);
    onboarding.setInstitution(new Institution());

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(2))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(1));
  }

  @Test
  void usersPgOrchestrator_whenStatusRequest() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.REQUEST);
    onboarding.setWorkflowType(WorkflowType.USERS_PG);
    onboarding.setInstitution(new Institution());

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(0))
            .callActivity(captorActivity.capture(), any(), any(), any());
  }

  @Test
  void createInstitutionAndPersistInstitutionId() {

    final String institutionId = "institutionId";
    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    when(completionService.createInstitutionAndPersistInstitutionId(any()))
            .thenReturn(institutionId);

    String actualInstitutionId =
            function.createInstitutionAndPersistInstitutionId(onboardingStringBase, executionContext);

    assertEquals(institutionId, actualInstitutionId);
    verify(completionService, times(1)).createInstitutionAndPersistInstitutionId(any());
  }

  @Test
  void rejectOutdatingOnboardings() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(completionService).rejectOutdatedOnboardings(any());

    function.rejectOutdatedOnboardings(onboardingStringBase, executionContext);

    verify(completionService, times(1)).rejectOutdatedOnboardings(any());
  }

  @Test
  void createOnboarding() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(completionService).persistOnboarding(any());

    function.createOnboarding(onboardingStringBase, executionContext);

    verify(completionService, times(1)).persistOnboarding(any());
  }

  @Test
  void sendCompletedEmail() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(completionService).sendCompletedEmail(any());

    function.sendMailCompletion(onboardingWorkflowString, executionContext);

    verify(completionService, times(1)).sendCompletedEmail(any());
  }

  @Test
  void sendMailRejection() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(completionService).sendMailRejection(any(), any());

    function.sendMailRejection(onboardingStringBase, executionContext);

    verify(completionService, times(1)).sendMailRejection(any(), any());
  }

  @Test
  void createUsersOnboarding() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(completionService).persistUsers(any());

    function.createOnboardedUsers(onboardingStringBase, executionContext);

    verify(completionService, times(1)).persistUsers(any());
  }

  @Test
  void createAggregateOnboardingRequest() {
    final String onboardingAggregateOrchestratorInputString = "{\"productId\":\"prod-io\"}";

    String onboardingId = "id";
    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    when(completionService.createAggregateOnboardingRequest(any())).thenReturn(onboardingId);

    String response =
            function.createAggregateOnboardingRequest(
                    onboardingAggregateOrchestratorInputString, executionContext);

    Assertions.assertEquals(onboardingId, response);
    Mockito.verify(completionService, times(1)).createAggregateOnboardingRequest(any());
  }

  @Test
  void createDelegationForAggregation() {
    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    when(completionService.createDelegation(any())).thenReturn("delegationId");

    String delegationId =
            function.createDelegationForAggregation(onboardingStringBase, executionContext);

    Assertions.assertEquals("delegationId", delegationId);
    verify(completionService, times(1)).createDelegation(any());
  }

  @Test
  void createDelegationForAggregationIncrement() {
    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    when(completionService.existsDelegation(any())).thenReturn("true");

    String exists = function.existsDelegation(onboardingStringBase, executionContext);

    Assertions.assertEquals("true", exists);
    verify(completionService, times(1)).existsDelegation(any());
  }

  @Test
  void verifyOnboardingAggregate() {

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    when(completionService.verifyOnboardingAggregate(any())).thenReturn(onboardingWithoutInstitutionIdString);

    String onboardingAggregate = function.verifyOnboardingAggregate(onboardingStringBase, executionContext);

    Assertions.assertEquals(onboardingAggregate, onboardingWithoutInstitutionIdString);
    verify(completionService, times(1)).verifyOnboardingAggregate(any());
  }

  @Test
  void sendTestEmail() {
    @SuppressWarnings("unchecked")
    final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

    doAnswer(
            (Answer<HttpResponseMessage.Builder>)
                    invocation -> {
                      HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                      return new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
                              .status(status);
                    })
            .when(req)
            .createResponseBuilder(any(HttpStatus.class));

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    doNothing().when(completionService).sendTestEmail(executionContext);

    function.sendTestEmail(req, executionContext);

    verify(completionService, times(1)).sendTestEmail(executionContext);
  }

  @Test
  void retrieveAggregates() {
    List<DelegationResponse> delegationResponseList = new ArrayList<>();
    DelegationResponse delegationResponse = new DelegationResponse();
    delegationResponse.setId("id");
    delegationResponse.setInstitutionId("institutionId");
    delegationResponseList.add(delegationResponse);

    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    when(completionService.retrieveAggregates(any())).thenReturn(delegationResponseList);

    String delegationsList = function.retrieveAggregates(onboardingStringBase, executionContext);

    Assertions.assertEquals(
            Utils.getDelegationResponseListString(objectMapper, delegationResponseList),
            delegationsList);
    verify(completionService, times(1)).retrieveAggregates(any());
  }

  @Test
  void onboardingOrchestratorUsersEaPending() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setWorkflowType(WorkflowType.USERS_EA);

    List<DelegationResponse> delegationResponseList = new ArrayList<>();
    DelegationResponse delegationResponse = new DelegationResponse();
    delegationResponse.setId("id");
    delegationResponseList.add(delegationResponse);
    delegationResponseList.add(delegationResponse);

    TaskOrchestrationContext orchestrationContext =
            mockTaskOrchestrationContextForUsersEa(onboarding, delegationResponseList);
    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(6))
            .callActivity(captorActivity.capture(), any(), any(), any());

    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(RETRIEVE_AGGREGATES_ACTIVITY, captorActivity.getAllValues().get(1));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(3));
    assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(4));
    assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(5));

    verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);
  }

  @Test
  void onboardingsOrchestratorForApproveGpu() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.REQUEST);
    onboarding.setWorkflowType(WorkflowType.FOR_APPROVE_GPU);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(1))
            .callActivity(captorActivity.capture(), any(), any(), any());

    assertEquals(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY, captorActivity.getAllValues().get(0));

    Mockito.verify(orchestrationContext, times(1))
            .callSubOrchestrator(eq(BUILD_ATTACHMENTS_SAVE_TOKENS_ACTIVITY), any(), any());

    verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.TOBEVALIDATED);
  }

  @Test
  void onboardingsOrchestratorForApproveGpuWhenIsPending() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setInstitution(new Institution());
    onboarding.setWorkflowType(WorkflowType.FOR_APPROVE_GPU);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(5))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
    assertEquals(STORE_ONBOARDING_ACTIVATEDAT, captorActivity.getAllValues().get(3));
    assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(4));

    verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.COMPLETED);
  }

  @Test
  void onboardingsOrchestratorForApproveGpuWhenToBeValidated() {
    Onboarding onboarding = new Onboarding();
    List<User> users = new ArrayList<>();
    User user = new User();
    users.add(user);
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);
    onboarding.setWorkflowType(WorkflowType.FOR_APPROVE_GPU);
    onboarding.setUsers(users);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);

    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(4))
            .callActivity(captorActivity.capture(), any(), any(), any());
    assertEquals(BUILD_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(0));
    assertEquals(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, captorActivity.getAllValues().get(1));
    assertEquals(SEND_MAIL_REGISTRATION_FOR_CONTRACT_WHEN_APPROVE_ACTIVITY, captorActivity.getAllValues().get(2));
    assertEquals(SEND_MAIL_REGISTRATION_FOR_USER, captorActivity.getAllValues().get(3));

    verify(service, times(1))
            .updateOnboardingStatus(onboarding.getId(), OnboardingStatus.PENDING);
  }

  @Test
  void onboardingsOrchestratorForImportOfAggregator() {
    // given
    Onboarding onboarding = new Onboarding();
    onboarding.setId("onboardingId");
    onboarding.setStatus(OnboardingStatus.PENDING);
    onboarding.setWorkflowType(WorkflowType.IMPORT_AGGREGATION);
    Institution institution = new Institution();
    institution.setId("id");
    onboarding.setInstitution(institution);

    List<AggregateInstitution> aggregateInstitutions = new ArrayList<>();
    AggregateInstitution aggregateInstitution = new AggregateInstitution();
    aggregateInstitution.setDescription("description");
    aggregateInstitutions.add(aggregateInstitution);
    onboarding.setAggregates(aggregateInstitutions);

    TaskOrchestrationContext orchestrationContext = mockTaskOrchestrationContext(onboarding);
    Task<String> verifyTask = mockTaskWithValue(onboardingWithoutInstitutionIdString);
    when(orchestrationContext.callActivity(eq(VERIFY_ONBOARDING_AGGREGATE_ACTIVITY), any(), eq(String.class)))
            .thenReturn(verifyTask);
    // when
    function.onboardingsOrchestrator(orchestrationContext, executionContext);

    // then
    ArgumentCaptor<String> captorActivity = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> captorActivitySubOrchestrator = ArgumentCaptor.forClass(String.class);
    verify(orchestrationContext, times(4))
            .callActivity(captorActivity.capture(), any(), any(), any());
    verify(orchestrationContext, times(1))
            .callSubOrchestrator(captorActivitySubOrchestrator.capture(), any(), any());

    assertEquals(CREATE_INSTITUTION_ACTIVITY, captorActivity.getAllValues().get(0));
    assertEquals(CREATE_ONBOARDING_ACTIVITY, captorActivity.getAllValues().get(1));
    assertEquals(CREATE_USERS_ACTIVITY, captorActivity.getAllValues().get(2));
    assertEquals(ONBOARDINGS_AGGREGATE_ORCHESTRATOR, captorActivitySubOrchestrator.getAllValues().get(0));
    assertEquals(SEND_MAIL_COMPLETION_ACTIVITY, captorActivity.getAllValues().get(3));

  }

  private Product createDummyProduct() {
    Product product = new Product();
    product.setTitle("Title");
    product.setId("test");
    product.setInstitutionContractMappings(createDummyContractTemplateInstitution());
    product.setUserContractMappings(createDummyContractTemplateInstitution());

    return product;
  }

  @SuppressWarnings("unchecked")
  private <T> Task<T> mockTaskWithValue(T value) {
    Task<T> task = mock(Task.class);
    try {
      when(task.await()).thenReturn(value);
    } catch (Exception e) {
      fail("Failed mocking task.await(): " + e.getMessage());
    }
    return task;
  }

  private static Map<String, ContractTemplate> createDummyContractTemplateInstitution() {
    Map<String, ContractTemplate> institutionTemplate = new HashMap<>();

    List<AttachmentTemplate> attachments = new ArrayList<>();

    AttachmentTemplate attachmentTemplate = new AttachmentTemplate();
    attachmentTemplate.setTemplatePath("path");
    attachmentTemplate.setWorkflowState(OnboardingStatus.REQUEST);
    attachmentTemplate.setWorkflowType(List.of(WorkflowType.FOR_APPROVE));

    attachments.add(attachmentTemplate);

    ContractTemplate conctractTemplate = new ContractTemplate();
    conctractTemplate.setContractTemplatePath("example");
    conctractTemplate.setContractTemplateVersion("version");
    conctractTemplate.setAttachments(attachments);

    institutionTemplate.put(Product.CONTRACT_TYPE_DEFAULT, conctractTemplate);
    return institutionTemplate;
  }

}
