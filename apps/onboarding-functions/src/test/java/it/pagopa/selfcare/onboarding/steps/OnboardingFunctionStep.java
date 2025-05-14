package it.pagopa.selfcare.onboarding.steps;

import static it.pagopa.selfcare.onboarding.steps.IntegrationFunctionProfile.storeIntoMongo;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Vertx;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.openapi.quarkus.core_json.api.InstitutionApi;

@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"it.pagopa.selfcare.onboarding"},
        plugin = {
                "html:target/cucumber-report/cucumber.html",
                "json:target/cucumber-report/cucumber.json"
        },
        tags = "@Onboarding")
@TestProfile(IntegrationFunctionProfile.class)
@Slf4j
public class OnboardingFunctionStep extends CucumberQuarkusTest {

  private ValidatableResponse validatableResponse;

  @InjectMock @RestClient InstitutionApi institutionApi;

  static MongoDatabase mongoDatabase;

  private RequestSpecification request;
  private Response response;
  private String functionUrl;
  private String endpoint;
  private String onboardingId;
  private int timeout;

  @Inject
  OnboardingRepository onboardingRepository;

  public static void main(String[] args) {
    runMain(OnboardingFunctionStep.class, args);
  }

  @BeforeAll
  static void setup() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    Vertx vertx = Vertx.vertx();
    vertx
            .getOrCreateContext()
            .config()
            .put("quarkus.vertx.event-loop-blocked-check-interval", 5000);

    initDb();

    log.debug("Init completed");
  }

  private static void initDb() {
    mongoDatabase = IntegrationFunctionProfile.getMongoClientConnection();
    Onboarding onboardingContractRegistration = createOnboardingContractRegistration();
    Onboarding onboardingForApprove = createOnboardingForApprove();
    storeIntoMongo(onboardingContractRegistration, "onboardings");
    storeIntoMongo(onboardingForApprove, "onboardings");
    // verify
    assertNotNull(onboardingContractRegistration.getId());
    assertNotNull(onboardingForApprove.getId());
    mongoDatabase.getCollection("onboardings")
            .createIndex(Indexes.ascending("createdAt"));
  }

  @Given("una Azure Function Quarkus configurata per gestire richieste HTTP")
  public void setupAzureFunction() {
    request = RestAssured.given()
            .contentType("application/json");

    functionUrl = "http://localhost:7071/api/StartOnboardingOrchestration";
  }


  @Given("The endpoint is {string}")
  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  @When("I send a GET request parameters:")
  public void sendGetRequest(Map<String, String> params) {
    setOnboardingId(params);
    setTimeout(params);
    for (Map.Entry<String, String> entry : params.entrySet()) {
      request
              .queryParam(entry.getKey(), entry.getValue());
    }
    response = request
            .when()
            .get(endpoint);
  }

  private void setOnboardingId(Map<String, String> params) {
    assertTrue(params.containsKey("onboardingId"), "Parameter onboardingId not found");
    onboardingId = params.get("onboardingId");
  }

  private void setTimeout(Map<String, String> params) {
    if(params.containsKey("timeout")) {
      timeout = Integer.parseInt(params.get("timeout"));
    }
  }

  @When("invio una richiesta POST con payload {string}")
  public void sendPostRequest(String payload) {
    response = request
            .body(payload)
            .when()
            .post(functionUrl);
  }

  @Then("la risposta dovrebbe avere status code {int}")
  public void verifyStatusCode(int expectedStatusCode) {
    Assertions.assertEquals(expectedStatusCode, response.getStatusCode(),
            "Status code non corrispondente");
  }

  @Then("la risposta dovrebbe contenere {string}")
  public void verifyResponseBody(String expectedResponse) {
    response.then().body(Matchers.containsString(expectedResponse));
  }

  @Then("on db the status of onboarding is {status}")
  public void checkStatus(String expectedStatus) {
    boolean conditionMet = waitForCondition(() -> {
      Optional<Onboarding> optional = onboardingRepository.findByIdOptional(onboardingId);
      return optional.isPresent() && expectedStatus.equals(optional.get().getStatus().name());
    }, timeout, TimeUnit.SECONDS);
    assertTrue(conditionMet, "Onboarding status not updated to " + expectedStatus + " within the timeout period");
  }

  private boolean waitForCondition(CheckCondition condition, long timeout, TimeUnit unit) {
    long endTime = System.nanoTime() + unit.toNanos(timeout);
    while (System.nanoTime() < endTime) {
      if (condition.check()) {
        return true;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  @FunctionalInterface
  private interface CheckCondition {
    boolean check();
  }

  @AfterAll
  static void destroyDatabase() {
    mongoDatabase.drop();
  }

  private static Onboarding createOnboardingContractRegistration() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId(UUID.fromString("89ad7142-24bb-48ad-8504-9c9232137e85").toString());
    onboarding.setProductId("prod-io");
    onboarding.setStatus(OnboardingStatus.REQUEST);
    onboarding.setCreatedAt(LocalDateTime.now());
    onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);

    Institution institution = new Institution();
    institution.setOrigin(Origin.IPA);
    institution.setOriginId("c_l186");
    institution.setDescription("Comune di Tocco da Casauria");
    institution.setTaxCode("00231830688");
    institution.setInstitutionType(InstitutionType.PA);
    onboarding.setInstitution(institution);

    Billing billing = new Billing();
    billing.setRecipientCode("UFD333");
    onboarding.setBilling(billing);

    User user = new User();
    user.setId("actual-user-id");
    user.setRole(PartyRole.MANAGER);
    onboarding.setUsers(List.of(user));

    return onboarding;
  }

  private static Onboarding createOnboardingForApprove() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId(UUID.fromString("89ad7142-24bb-48ad-8502-9c9232137e85").toString());
    onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());
    onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);
    onboarding.setCreatedAt(LocalDateTime.now());
    onboarding.setWorkflowType(WorkflowType.FOR_APPROVE);

    Institution institution = new Institution();
    institution.setOrigin(Origin.SELC);
    institution.setOriginId("10293847565");
    institution.setDescription("Test 1");
    institution.setTaxCode("10293847565");
    institution.setInstitutionType(InstitutionType.PSP);
    onboarding.setInstitution(institution);

    Billing billing = new Billing();
    billing.setRecipientCode("A1B2C3");
    onboarding.setBilling(billing);

    User user = new User();
    user.setId("actual-user-id");
    user.setRole(PartyRole.MANAGER);
    onboarding.setUsers(List.of(user));

    return onboarding;
  }

}
