package it.pagopa.selfcare.onboarding.steps;

import static io.restassured.RestAssured.given;
import static it.pagopa.selfcare.onboarding.steps.IntegrationFunctionProfile.storeIntoMongo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasKey;
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
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.entity.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

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
  private static ObjectMapper objectMapper;
  private static String tokenTest;
  private static final String JWT_BEARER_TOKEN_ENV = "custom.jwt-token-test";

  static MongoDatabase mongoDatabase;

  private RequestSpecification request;
  private Response response;

  public static void main(String[] args) {
    runMain(OnboardingFunctionStep.class, args);
  }

  @BeforeAll
  static void setup() {
    tokenTest = ConfigProvider.getConfig().getValue(JWT_BEARER_TOKEN_ENV, String.class);
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    initDb();

    log.debug("Init completed");
  }

  @BeforeEach
  void resetRestAssured() {
    RestAssured.reset();
  }

  private static void initDb() {
    mongoDatabase = IntegrationFunctionProfile.getMongoClientConnection();
    Onboarding onboarding = createDummyOnboarding();
    storeIntoMongo(onboarding, "onboardings");

    Onboarding duplicatedOnboardingPA = createOnboardingForConflictScenario();
    storeIntoMongo(duplicatedOnboardingPA, "onboardings");

    Token token = createDummyToken();
    storeIntoMongo(token, "tokens");

    // verify
    assertNotNull(onboarding.getId());
    assertNotNull(duplicatedOnboardingPA.getId());
    mongoDatabase.getCollection("onboardings").createIndex(Indexes.ascending("createdAt"));
  }

  @Given("Preparing the invocation of {string} HTTP call")
  public void setupCall(String functionName) {
    RestAssured.baseURI = "http://localhost:9090";
    RestAssured.basePath = String.format("/api/%s", functionName);
  }

  @When("I send a GET request with onboardingId {string}")
  public void sendPostRequest(String onboardingId) {
    response =
        given()
            .log()
            .all()
            .queryParam("onboardingId", onboardingId)
            .when()
            .get()
            .then()
            .log()
            .all()
            .extract()
            .response();
  }

  @Then("the response should have status code {int}")
  public void verifyStatusCode(int expectedStatusCode) {
    assertEquals(expectedStatusCode, response.getStatusCode(), "Status code non corrispondente");
  }

  @Then("the answer should contain {string}")
  public void verifyResponseBody(String expectedResponse) {
    List<String> expectedValue = List.of(expectedResponse.split(","));

    response
        .then()
        .assertThat()
        .body("$", allOf(expectedValue.stream().map(key -> hasKey(key)).toArray(Matcher[]::new)));
  }


  @AfterAll
  static void destroyDatabase() {
    mongoDatabase.drop();
  }

  // utils
  private static Onboarding createDummyOnboarding() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId(UUID.fromString("89ad7142-24bb-48ad-8504-9c9231137e85").toString());
    onboarding.setProductId("prod-pagopa");
    onboarding.setCreatedAt(LocalDateTime.now());

    Institution institution = new Institution();
    institution.setTaxCode("taxCode");
    institution.setSubunitCode("subunitCode");
    institution.setInstitutionType(InstitutionType.PSP);
    onboarding.setInstitution(institution);

    Billing billing = new Billing();
    billing.setRecipientCode("RC000");
    onboarding.setBilling(billing);

    User user = new User();
    user.setId("actual-user-id");
    user.setRole(PartyRole.MANAGER);
    onboarding.setUsers(List.of(user));

    return onboarding;
  }

  private static Onboarding createOnboardingForConflictScenario() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId(UUID.randomUUID().toString());
    onboarding.setProductId("prod-io");
    onboarding.setStatus(OnboardingStatus.COMPLETED);
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

  private static Token createDummyToken() {
    Token token = new Token();
    token.setId(UUID.fromString("89ad7142-24bb-48ad-8504-9c9231137e85").toString());
    token.setProductId("prod-pagopa");
    token.setCreatedAt(LocalDateTime.now());
    return token;
  }

}
