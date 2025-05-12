@Onboarding
Feature: Onboarding collection

  Scenario: Verifica la corretta elaborazione di un evento HTTP
    Given una Azure Function Quarkus configurata per gestire richieste HTTP
    When invio una richiesta POST con payload '{"nome": "Test", "valore": 42}'
    Then la risposta dovrebbe avere status code 200
    And la risposta dovrebbe contenere '{"status": "success", "processedValue": 42}'

  Scenario: Can't perform start orchestration for not existing onbaording
    Given The endpoint is "/api/startOnboardingOrchestration"
    When I send a GET request parameters:
      | onboardingId | 12345 |
      | timeout      | 50000 |
    Then the response status code should be 404
    And the response should contain the text "Onboarding with id 12345 not found!"

  Scenario: Successfully start orchestration for onbaording with workflowType CONTRACT_REGISTRATION
    Given The endpoint is "/api/startOnboardingOrchestration"
    When I send a GET request parameters:
      | onboardingId | 89ad7142-24bb-48ad-8504-9c9232137e85 |
      | timeout      | null                                 |
    Then the response status code should be 202
    And on db the status of onboarding is "PENDING"

  Scenario: Successfully complete onbaording with workflowType CONTRACT_REGISTRATION
    Given The endpoint is "/api/startOnboardingOrchestration"
    When I send a GET request parameters:
      | onboardingId | 89ad7142-24bb-48ad-8504-9c9232137e85 |
      | timeout      | null
    Then the response status code should be 202
    And on db the status of onboarding is "COMPLETED"

  Scenario: Successfully start orchestration for onbaording with workflowType FOR_APPROVE
    Given The endpoint is "/api/startOnboardingOrchestration"
    When I send a GET request parameters:
      | onboardingId | 89ad7142-24bb-48ad-8502-9c9232137e85 |
      | timeout      | null                                 |
    Then the response status code should be 202
    And on db the status of onboarding is "TOBEVALIDATED"

  Scenario: Successfully validate onbaording with workflowType FOR_APPROVE
    Given The endpoint is "/api/startOnboardingOrchestration"
    When I send a GET request parameters:
      | onboardingId | 89ad7142-24bb-48ad-8502-9c9232137e85 |
      | timeout      | null                                 |
    Then the response status code should be 202
    And on db the status of onboarding is "PENDING"

  Scenario: Successfully complete onbaording with workflowType FOR_APPROVE
    Given The endpoint is "/api/startOnboardingOrchestration"
    When I send a GET request parameters:
      | onboardingId | 89ad7142-24bb-48ad-8502-9c9232137e85 |
      | timeout      | null
    Then the response status code should be 202
    And on db the status of onboarding is "COMPLETED"