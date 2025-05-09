@Onboarding
Feature: Onboarding collection

  Scenario: Verifica la corretta elaborazione di un evento HTTP
    Given una Azure Function Quarkus configurata per gestire richieste HTTP
    When invio una richiesta POST con payload '{"nome": "Test", "valore": 42}'
    Then la risposta dovrebbe avere status code 200
    And la risposta dovrebbe contenere '{"status": "success", "processedValue": 42}'

  Scenario: Can't perform start orchestration for not existing onbaording
    Given I have a request object '{"onboardingId": "12345"}'
    When I send a POST request for user to "/api/startOnboardingOrchestration" with this request
    Then the response status code should be 404
    And the response should contain the text "Onboarding with id 12345 not found!"

  Scenario: Successfully start orchestration for onbaording with workflowType CONTRACT_REGISTRATION
    Given I have a request object '{"onboardingId": "89ad7142-24bb-48ad-8504-9c9232137e85"}'
    When I send a POST request for user to "/api/startOnboardingOrchestration" with this request
    Then the response status code should be 200
    And the response should contain the text "Onboarding with id 12345 not found!"
    And on db the status of onboarding is "PENDING"

  Scenario: Successfully complete orchestration for onbaording with workflowType CONTRACT_REGISTRATION
    Given I have a request object '{"onboardingId": "89ad7142-24bb-48ad-8504-9c9232137e85"}'
    When I send a POST request for user to "/api/startOnboardingOrchestration" with this request
    Then the response status code should be 200
    And the response should contain the text "Onboarding with id 12345 not found!"
    And on db the status of onboarding is "COMPLETED"