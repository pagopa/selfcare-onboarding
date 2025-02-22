package it.pagopa.selfcare.onboarding.steps;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.mongodb.MongoTestResource;
import io.restassured.response.ValidatableResponse;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.controller.OnboardingController;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

@CucumberOptions(
    features = "src/test/resources/features",
    plugin = {
      "html:target/cucumber-report/cucumber.html",
      "json:target/cucumber-report/cucumber.json"
    })
@TestHTTPEndpoint(OnboardingController.class)
@QuarkusTestResource(MongoTestResource.class)
@TestProfile(IntegrationProfile.class)
@Slf4j
public class OnboardingStep extends CucumberQuarkusTest {

  private static MongoDatabase database;
  private static MongoCollection<Document> collection;

  private ValidatableResponse validatableResponse;

  private Onboarding onboarding;

  private static String tokenTest;
  private static final String JWT_BEARER_TOKEN_ENV = "custom.jwt-token-test";

  public static void main(String[] args) {
    runMain(OnboardingStep.class, args);
  }

  @BeforeAll
  public static void setup() {
    database = MongoClients.create("mongodb://localhost:27017").getDatabase("dummyOnboarding");
    collection = database.getCollection("onboardings");
    tokenTest = ConfigProvider.getConfig().getValue(JWT_BEARER_TOKEN_ENV, String.class);
    log.debug("Init completed");
  }

  @BeforeEach
  public void initData() {
    onboarding = createDummyOnboarding();
    onboarding.persist().await().indefinitely();

    // verify
    assertNotNull(onboarding.getId());
  }

  @Given(
      "I have an onboarding record with onboardingId {string} the current recipient code is {string}")
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

  @AfterEach
  public void cleanDb() {
    onboarding.delete();
  }

  @AfterAll
  public static void cleanup() {
    database.drop();
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
}
