Feature: Onboarding collection

  Scenario: Successfully update recipient code by onboarding id
    Given I have an onboarding record with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e85" the current recipient code is "RC001"
    When I send a PUT request to "/{onboardingId}/recipient-code" with "89ad7142-24bb-48ad-8504-9c9231137e85" and "RC001"
    Then the response status code should be 204

  Scenario: Can't update recipient code with invalid onboardingId
    Given I have an onboarding record with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e90" the current recipient code is "RC001"
    When I send a PUT request to "/{onboardingId}/recipient-code" with "89ad7142-24bb-48ad-8504-9c9231137e90" and "RC001"
    Then the response status code should be 400