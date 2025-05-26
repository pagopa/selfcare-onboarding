@Onboarding
Feature: Onboarding collection

  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct GPU request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e85"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status        |
      | TOBEVALIDATED |
      | PENDING       |
      | COMPLETED     |

  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect GPU request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e86"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "FAILED"

  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct PT request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e87"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status        |
      | TOBEVALIDATED |
      | COMPLETED     |

  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect GPU request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e88"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "FAILED"

  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct PSP request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e89"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status    |
      | PENDING   |
      | COMPLETED |

  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect PSP request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e90"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "FAILED"

  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct PA request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9232137e85"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status    |
      | PENDING   |
      | COMPLETED |

  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect PA request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9232137e97"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "FAILED"

  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct FOR_APPROVE request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8502-9c9232137e95"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status        |
      | TOBEVALIDATED |
      | PENDING       |
      | COMPLETED     |

  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect FOR_APPROVE request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e99"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "FAILED"

  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct PSP request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e91"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status    |
      | PENDING   |
      | COMPLETED |

  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect PSP request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e92"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "FAILED"