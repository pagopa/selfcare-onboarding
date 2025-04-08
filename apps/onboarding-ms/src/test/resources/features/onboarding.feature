@cucumberOnboarding
Feature: Onboarding collection

  Scenario: Successfully update recipient code by onboarding id
    Given I have an onboarding record with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e85" the current recipient code is "RC001"
    When I send a PUT request to "/{onboardingId}/recipient-code" with "89ad7142-24bb-48ad-8504-9c9231137e85" and "RC001"
    Then the response status code should be 204

  Scenario: Can't update recipient code with invalid onboardingId
    Given I have an onboarding record with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e90" the current recipient code is "RC001"
    When I send a PUT request to "/{onboardingId}/recipient-code" with "89ad7142-24bb-48ad-8504-9c9231137e90" and "RC001"
    Then the response status code should be 400

  Scenario: Can't perform onboarding request for an empty object
    Given I have an empty request object
    When I send a POST request to "/pa" with empty body
    Then the response status code should be 400

  Scenario: Can't perform onboarding request for an invalid object
    Given I have a request object named "invalid_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "institutionData is required"

  Scenario: Can't perform onboarding request with missing origin and originId
    Given I have a request object named "empty_origin_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "origin is required"
    And the response should contain the text "originId is required"

  Scenario: Can't perform onboarding request with missing users node
    Given I have a request object named "empty_users_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "at least one user is required"

  Scenario: Can't perform onboarding request with missing digitalAddress attribute
    Given I have a request object named "empty_pec_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "digitalAddress is required"

  Scenario: Successfully store onboarding in status REQUEST
    Given I have a request object named "success_pa_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"

  Scenario: Can't perform onboarding with parent not completed
    Given I have a request object named "invalid_child_product_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "Parent product prod-io not onboarded for institution having externalId 83001010616"

  Scenario: Can't perform onboarding for the same product, taxCode and workflowType
    Given I have a request object named "duplicated_onboarding_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 409

  Scenario: Can't perform onboarding for prod-io-premium with not allowed pricingPlan
    Given I have a request object named "invalid_io_premium_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "onboarding pricing plan for io-premium is not allowed"

  Scenario: Can't perform onboarding for GSP without additional information
    Given I have a request object named "invalid_gsp_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "Additional Information is required when institutionType is GSP and productId is pagopa"

  Scenario: Can't perform onboarding for GSP without note field
    Given I have a request object named "invalid_gsp_note_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "Other Note is required when other boolean are false"

  Scenario: Can't perform onboarding request for invalid digitalAddress (different from IPA registry one)
    Given I have a request object named "invalid_proxy_pec_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "Field digitalAddress or description are not valid"

  Scenario: Can't perform onboarding request for invalid recipient code (different from IPA registry one)
    Given I have a request object named "invalid_proxy_recipient_code_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 404
    And the response should contain the text "UO UFO5PP not found"

  Scenario: Successfully perform onboarding request for AOO
    Given I have a request object named "success_aoo_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"

  Scenario: Successfully perform onboarding request for UO
    Given I have a request object named "success_uo_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"

  Scenario: Can't perform onboarding request for UO with invalid recipientCode
    Given I have a request object named "invalid_recipient_code_uo_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "Recipient code not linked to any institution"

  Scenario: Can't perform onboarding request for UO with denied billing
    Given I have a request object named "invalid_billing_uo_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "Recipient code linked to an institution with invoicing service not active"