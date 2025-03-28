package it.pagopa.selfcare.onboarding.steps;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.ValidatableResponse;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.controller.OnboardingController;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openapi.quarkus.onboarding_functions_json.api.OrchestrationApi;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;

@CucumberOptions(
        features = "src/test/resources/features",
        plugin = {
                "html:target/cucumber-report/cucumber.html",
                "json:target/cucumber-report/cucumber.json"
        })
@TestHTTPEndpoint(OnboardingController.class)
@TestProfile(IntegrationProfile.class)
@Slf4j
public class OnboardingStep extends CucumberQuarkusTest {

  private static MongoDatabase database;
  private static MongoCollection<Document> collection;
  private ValidatableResponse validatableResponse;
  private Onboarding onboarding;
  private Onboarding duplicatedOnboardingPA;
  private static ObjectMapper objectMapper;
  private static String tokenTest;
  private static final String JWT_BEARER_TOKEN_ENV = "custom.jwt-token-test";

  @InjectMock
  @RestClient
  OrchestrationApi orchestrationApi;

  public static void main(String[] args) {
    runMain(OnboardingStep.class, args);
  }

  @BeforeAll
  static void setup() {
    tokenTest = ConfigProvider.getConfig().getValue(JWT_BEARER_TOKEN_ENV, String.class);
    objectMapper = new ObjectMapper();
    Vertx vertx = Vertx.vertx();
    vertx.getOrCreateContext().config().put("quarkus.vertx.event-loop-blocked-check-interval", 5000);
    log.debug("Init completed");

  }

  @BeforeEach
    void initData() {
    onboarding = createDummyOnboarding();
    onboarding.persist().await().indefinitely();
    duplicatedOnboardingPA = createOnboardingForConflictScenario();
    duplicatedOnboardingPA.persist().await().indefinitely();
    // verify
    assertNotNull(onboarding.getId());
    assertNotNull(duplicatedOnboardingPA.getId());
    when(orchestrationApi.apiStartOnboardingOrchestrationGet(any(), any()))
            .thenReturn(Uni.createFrom().item(new OrchestrationResponse()));
  }

  @Given("I have an onboarding record with onboardingId {string} the current recipient code is {string}")
  public void givenDataCheck(String onboardingId, String recipientCode) {
    assertNotNull(onboardingId);
    assertNotNull(recipientCode);
  }

  @When("I send a PUT request to {string} with {string} and {string}")
  public void doCallApi(String url, String onboardingId, String recipientCode) {
    validatableResponse =
            given()
                    .header(
                            "Authorization",
                            "Bearer "
                                    + tokenTest)
                    .pathParam("onboardingId", onboardingId)
                    .queryParam("recipientCode", recipientCode)
                    .when()
                    .put(url)
                    .then();
  }

  @Then("the response status code should be {int}")
  public void verifyStatusCodeResponse(int statusCode) {
    assertEquals(statusCode, validatableResponse.extract().statusCode());
  }

  @Then("the response should contain the text {string}")
  public void verifyResponseText(String expectedText) {
    String responseBody = validatableResponse.extract().body().asString();
    assertTrue(responseBody.contains(expectedText), "Response does not contain expected text");
  }

  @Given("I have an empty request object")
  public void givenEmptyRequst() {
    OnboardingDefaultRequest request = new OnboardingDefaultRequest();
    Arrays.stream(request.getClass().getDeclaredFields()).forEach(field -> {
      field.setAccessible(true);
      try {
        assertNull(field.get(request), "L'attributo " + field.getName() + " dovrebbe essere nullo");
      } catch (IllegalAccessException e) {
        fail("Impossibile accedere all'attributo " + field.getName());
      }
    });
  }

  @Given("I have an invalid request object with attributes")
  public void givenInvalidRequest(String onboardingRequest) throws JsonProcessingException {
    OnboardingDefaultRequest request = objectMapper.readValue(onboardingRequest, OnboardingDefaultRequest.class);
    assertNotNull(request);
  }

  @Given("I have a request object")
  public void givenValidObject(String onboardingRequest) throws JsonProcessingException {
    OnboardingDefaultRequest request = objectMapper.readValue(onboardingRequest, OnboardingDefaultRequest.class);
    assertNotNull(request);
  }

  @Given("I have a valid request object")
  public void givenDuplicatedOnboardingPA(String onboardingRequest) throws JsonProcessingException {
    OnboardingDefaultRequest request = objectMapper.readValue(onboardingRequest, OnboardingDefaultRequest.class);
    assertNotNull(request);
  }

  @When("I send a POST request to {string} with empty body")
  @TestSecurity(user = "testUser", roles = {"admin", "user"})
  public void doCallApi(String url) {
    validatableResponse =
        given()
            .header("Authorization", "Bearer " + tokenTest)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new OnboardingDefaultRequest())
            .when()
            .post(url)
            .then();
  }

  @When("I send a POST request to {string} with the request body")
  @TestSecurity(user = "testUser", roles = {"admin", "user"})
  public void doCallApi(String url, String onboardingRequest) throws JsonProcessingException {
    OnboardingDefaultRequest request = objectMapper.readValue(onboardingRequest, OnboardingDefaultRequest.class);
    assertNotNull(request);
    validatableResponse =
            given()
                    .header("Authorization", "Bearer " + tokenTest)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post(url)
                    .then();
  }

  @When("I send a duplicated POST request to {string} with the request body")
  @TestSecurity(user = "testUser", roles = {"admin", "user"})
  public void doCallApiForConflictScenario(String url, String requestBody) throws JsonProcessingException {
    OnboardingDefaultRequest request = objectMapper.readValue(requestBody, OnboardingDefaultRequest.class);
    validatableResponse =
            given()
                    .header("Authorization", "Bearer " + tokenTest)
                    .body(request)
                    .when()
                    .post(url)
                    .then();
  }


  @AfterEach
  public void cleanDb() {
    onboarding.delete();
  }

  // utils
  private static Onboarding createDummyOnboarding() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId(UUID.fromString("89ad7142-24bb-48ad-8504-9c9231137e85").toString());
    onboarding.setProductId("prod-pagopa");

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
    onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);

    Institution institution = new Institution();
    institution.setOrigin(Origin.IPA);
    institution.setOriginId("c_l186");
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

}
