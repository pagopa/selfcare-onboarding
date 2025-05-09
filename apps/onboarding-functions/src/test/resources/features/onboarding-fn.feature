@Onboarding
Feature: Onboarding collection

  Scenario: Verifica la corretta elaborazione di un evento HTTP
    Given una Azure Function Quarkus configurata per gestire richieste HTTP
    When invio una richiesta POST con payload '{"nome": "Test", "valore": 42}'
    Then la risposta dovrebbe avere status code 200
    And la risposta dovrebbe contenere '{"status": "success", "processedValue": 42}'