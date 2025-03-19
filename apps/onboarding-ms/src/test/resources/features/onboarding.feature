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
    Given I have an invalid request object with attributes
          """
          {
              "productId": "prod-io"
          }
          """
    When I send a POST request to "/pa" with the request body
          """
          {
              "productId": "prod-io"
          }
          """
    Then the response status code should be 400
    And the response should contain the text "institutionData is required"

  Scenario: Can't perform onboarding request with missing origin and originId
    Given I have an invalid request object with attributes
           """
           {
               "productId": "prod-io",
               "users": [
                   {
                       "taxCode": "FRDSMN80A01F205Z",
                       "name": "Sigmund",
                       "surname": "Freud",
                       "email": "s.freud@test.email.it",
                       "role": "MANAGER"
                   },
                   {
                       "taxCode": "CLVTLI80A01F839V",
                       "name": "Italo",
                       "surname": "Calvino",
                       "email": "i.calvino@test.email.it",
                       "role": "DELEGATE"
                   }
               ],
               "institution": {

               }
           }
           """
    When I send a POST request to "/pa" with the request body
           """
           {
               "productId": "prod-io",
               "users": [
                   {
                       "taxCode": "FRDSMN80A01F205Z",
                       "name": "Sigmund",
                       "surname": "Freud",
                       "email": "s.freud@test.email.it",
                       "role": "MANAGER"
                   },
                   {
                       "taxCode": "CLVTLI80A01F839V",
                       "name": "Italo",
                       "surname": "Calvino",
                       "email": "i.calvino@test.email.it",
                       "role": "DELEGATE"
                   }
               ],
               "institution": {

               }
           }
           """
    Then the response status code should be 400
    And the response should contain the text "origin is required"
    And the response should contain the text "originId is required"

  Scenario: Can't perform onboarding request with missing users node
    Given I have an invalid request object with attributes
           """
           {
               "productId": "prod-io",
               "institution": {
                  "originId": "originId",
                  "origin": "IVASS"
               }
           }
           """
    When I send a POST request to "/pa" with the request body
          """
          {
               "productId": "prod-io",
               "institution": {
                  "originId": "originId",
                  "origin": "IVASS"
               }
          }
          """
    Then the response status code should be 400
    And the response should contain the text "at least one user is required"