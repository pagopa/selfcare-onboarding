@Onboarding
Feature: Onboarding collection

  Scenario: Verify correct invocation of the StartOnboardingOrchestration function via HTTP call
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call
    When I send a GET request with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e85"
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"