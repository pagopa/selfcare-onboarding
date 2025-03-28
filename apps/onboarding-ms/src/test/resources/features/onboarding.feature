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

  Scenario: Can't perform onboarding request with missing digitalAddress attribute
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
                  "originId": "originId",
                  "origin": "IVASS"
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
                  "originId": "originId",
                  "origin": "IVASS"
               }
           }
           """
    Then the response status code should be 400
    And the response should contain the text "digitalAddress is required"

  Scenario: Successfully store onboarding in status REQUEST
    Given I have a request object
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
                   "institutionType": "PA",
                   "taxCode": "83001010616",
                   "origin": "IPA",
                   "originId": "c_h423",
                   "city": "Roccamonfina",
                   "country": "IT",
                   "county": "CE",
                   "description": "Comune di Roccamonfina",
                   "digitalAddress": "protocollo.roccamonfina@asmepec.it",
                   "address": "Via Municipio N. 8",
                   "zipCode": "81035",
                   "geographicTaxonomies": [
                       {
                           "code": "ITA",
                           "desc": "ITALIA"
                       }
                   ],
                   "imported": false
               },
               "billing": {
                   "vatNumber": "83001010616",
                   "recipientCode": "UFO5LL",
                   "publicServices": true
               }
           }
           """
    When  I send a POST request to "/pa" with the request body
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
                             "institutionType": "PA",
                             "taxCode": "83001010616",
                             "origin": "IPA",
                             "originId": "c_h423",
                             "city": "Roccamonfina",
                             "country": "IT",
                             "county": "CE",
                             "description": "Comune di Roccamonfina",
                             "digitalAddress": "protocollo.roccamonfina@asmepec.it",
                             "address": "Via Municipio N. 8",
                             "zipCode": "81035",
                             "geographicTaxonomies": [
                                 {
                                     "code": "ITA",
                                     "desc": "ITALIA"
                                 }
                             ],
                             "imported": false
                         },
                         "billing": {
                             "vatNumber": "83001010616",
                             "recipientCode": "UFO5LL",
                             "publicServices": true
                         }
          }
          """
    Then the response status code should be 200

  Scenario: Can't perform onboarding with parent not completed
    Given I have a request object
          """
          {
               "productId": "prod-io-premium",
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
                   "institutionType": "PA",
                   "taxCode": "83001010616",
                   "origin": "IPA",
                   "originId": "c_h423",
                   "city": "Roccamonfina",
                   "country": "IT",
                   "county": "CE",
                   "description": "Comune di Roccamonfina",
                   "digitalAddress": "protocollo.roccamonfina@asmepec.it",
                   "address": "Via Municipio N. 8",
                   "zipCode": "81035",
                   "geographicTaxonomies": [
                       {
                           "code": "ITA",
                           "desc": "ITALIA"
                       }
                   ],
                   "imported": false
               },
               "billing": {
                   "vatNumber": "83001010616",
                   "recipientCode": "UFO5LL",
                   "publicServices": true
               }
           }
          """
    When  I send a POST request to "/pa" with the request body
          """
          {
                         "productId": "prod-io-premium",
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
                             "institutionType": "PA",
                             "taxCode": "83001010616",
                             "origin": "IPA",
                             "originId": "c_h423",
                             "city": "Roccamonfina",
                             "country": "IT",
                             "county": "CE",
                             "description": "Comune di Roccamonfina",
                             "digitalAddress": "protocollo.roccamonfina@asmepec.it",
                             "address": "Via Municipio N. 8",
                             "zipCode": "81035",
                             "geographicTaxonomies": [
                                 {
                                     "code": "ITA",
                                     "desc": "ITALIA"
                                 }
                             ],
                             "imported": false
                         },
                         "billing": {
                             "vatNumber": "83001010616",
                             "recipientCode": "UFO5LL",
                             "publicServices": true
                         }
          }
          """
    Then the response status code should be 400
    And the response should contain the text "Parent product prod-io not onboarded for institution having externalId 83001010616"

  Scenario: Can't perform onboarding for the same product, taxCode and workflowType
    Given I have a valid request object
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
                                          "institutionType": "PA",
                                          "taxCode": "00231830688",
                                          "origin": "IPA",
                                          "originId": "c_l186",
                                          "city": "ROMA",
                                          "country": "IT",
                                          "county": "RM",
                                          "digitalAddress": "digitalAddress@pec.it",
                                          "address": "Via dei Normanni, 5",
                                          "zipCode": "00184",
                                          "geographicTaxonomies": [
                                              {
                                                  "code": "ITA",
                                                  "desc": "ITALIA"
                                              }
                                          ],
                                          "imported": false
                                      },
                                      "billing": {
                                          "vatNumber": "80415740580",
                                          "recipientCode": "MU1E8J",
                                          "publicServices": true
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
                                          "institutionType": "PA",
                                          "taxCode": "00231830688",
                                          "origin": "IPA",
                                          "originId": "c_l186",
                                          "city": "ROMA",
                                          "country": "IT",
                                          "county": "RM",
                                          "digitalAddress": "digitalAddress@pec.it",
                                          "address": "Via dei Normanni, 5",
                                          "zipCode": "00184",
                                          "geographicTaxonomies": [
                                              {
                                                  "code": "ITA",
                                                  "desc": "ITALIA"
                                              }
                                          ],
                                          "imported": false
                                      },
                                      "billing": {
                                          "vatNumber": "80415740580",
                                          "recipientCode": "MU1E8J",
                                          "publicServices": true
                                      }
             }
             """
    Then the response status code should be 409

  Scenario: Can't perform onboarding for prod-io-premium with not allowed pricingPlan
    Given I have a valid request object
             """
             {
                   "productId": "prod-io-premium",
                   "pricingPlan": "c2",
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
                                          "institutionType": "PA",
                                          "taxCode": "00231830688",
                                          "origin": "IPA",
                                          "originId": "c_l186",
                                          "description": "Comune di Tocco da Casauria",
                                          "city": "Tocco Da Casauria",
                                          "country": "IT",
                                          "county": "PE",
                                          "digitalAddress": "comune.toccodacasauria@pec.arc.it",
                                          "address": "Largo Menna, 1",
                                          "zipCode": "65028",
                                          "geographicTaxonomies": [
                                              {
                                                  "code": "ITA",
                                                  "desc": "ITALIA"
                                              }
                                          ],
                                          "imported": false
                                      },
                                      "billing": {
                                          "vatNumber": "80415740580",
                                          "recipientCode": "UFD333",
                                          "publicServices": true
                                      }
             }
             """
    When I send a POST request to "/pa" with the request body
            """
             {
                   "productId": "prod-io-premium",
                   "pricingPlan": "c2",
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
                                          "institutionType": "PA",
                                          "taxCode": "00231830688",
                                          "origin": "IPA",
                                          "originId": "c_l186",
                                          "description": "Comune di Tocco da Casauria",
                                          "city": "Tocco Da Casauria",
                                          "country": "IT",
                                          "county": "PE",
                                          "digitalAddress": "comune.toccodacasauria@pec.arc.it",
                                          "address": "Largo Menna, 1",
                                          "zipCode": "65028",
                                          "geographicTaxonomies": [
                                              {
                                                  "code": "ITA",
                                                  "desc": "ITALIA"
                                              }
                                          ],
                                          "imported": false
                                      },
                                      "billing": {
                                          "vatNumber": "80415740580",
                                          "recipientCode": "UFD333",
                                          "publicServices": true
                                      }
             }
             """
    Then the response status code should be 400
    And the response should contain the text "onboarding pricing plan for io-premium is not allowed"

  Scenario: Can't perform onboarding request for invalid digitalAddress (different from IPA registry one)
    Given I have a request object
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
                   "institutionType": "PA",
                   "taxCode": "83001010616",
                   "origin": "IPA",
                   "originId": "c_h423",
                   "city": "Roccamonfina",
                   "country": "IT",
                   "county": "CE",
                   "description": "Comune di Roccamonfina",
                   "digitalAddress": "faked.pec@pec.it",
                   "address": "Via Municipio N. 8",
                   "zipCode": "81035",
                   "geographicTaxonomies": [
                       {
                           "code": "ITA",
                           "desc": "ITALIA"
                       }
                   ],
                   "imported": false
               },
               "billing": {
                   "vatNumber": "83001010616",
                   "recipientCode": "UFO5LL",
                   "publicServices": true
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
                   "institutionType": "PA",
                   "taxCode": "83001010616",
                   "origin": "IPA",
                   "originId": "c_h423",
                   "city": "Roccamonfina",
                   "country": "IT",
                   "county": "CE",
                   "description": "Comune di Roccamonfina",
                   "digitalAddress": "faked.pec@pec.it",
                   "address": "Via Municipio N. 8",
                   "zipCode": "81035",
                   "geographicTaxonomies": [
                       {
                           "code": "ITA",
                           "desc": "ITALIA"
                       }
                   ],
                   "imported": false
               },
               "billing": {
                   "vatNumber": "83001010616",
                   "recipientCode": "UFO5LL",
                   "publicServices": true
               }
           }
        """
    Then the response status code should be 400

  Scenario: Can't perform onboarding request for invalid recipient code (different from IPA registry one)
    Given I have a request object
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
                             "institutionType": "PA",
                             "taxCode": "83001010616",
                             "origin": "IPA",
                             "originId": "c_h423",
                             "city": "Roccamonfina",
                             "country": "IT",
                             "county": "CE",
                             "description": "Comune di Roccamonfina",
                             "digitalAddress": "protocollo.roccamonfina@asmepec.it",
                             "address": "Via Municipio N. 8",
                             "zipCode": "81035",
                             "geographicTaxonomies": [
                                 {
                                     "code": "ITA",
                                     "desc": "ITALIA"
                                 }
                             ],
                             "imported": false
                         },
                         "billing": {
                             "vatNumber": "83001010616",
                             "recipientCode": "UFO5PP",
                             "publicServices": true
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
                             "institutionType": "PA",
                             "taxCode": "83001010616",
                             "origin": "IPA",
                             "originId": "c_h423",
                             "city": "Roccamonfina",
                             "country": "IT",
                             "county": "CE",
                             "description": "Comune di Roccamonfina",
                             "digitalAddress": "protocollo.roccamonfina@asmepec.it",
                             "address": "Via Municipio N. 8",
                             "zipCode": "81035",
                             "geographicTaxonomies": [
                                 {
                                     "code": "ITA",
                                     "desc": "ITALIA"
                                 }
                             ],
                             "imported": false
                         },
                         "billing": {
                             "vatNumber": "83001010616",
                             "recipientCode": "UFO5PP",
                             "publicServices": true
                         }
          }
        """
    Then the response status code should be 404
    And the response should contain the text "UO UFO5PP not found"

  Scenario: Successfully perform onboarding request for AOO
    Given I have a request object
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
                   "institutionType": "PA",
                   "taxCode": "83001010616",
                   "origin": "IPA",
                   "originId": "AL7RYI5",
                   "subunitCode": "AL7RYI5",
                   "subunitType": "AOO",
                   "city": "Roccamonfina",
                   "country": "IT",
                   "county": "CE",
                   "description": "Comune di Roccamonfina",
                   "digitalAddress": "protocollo.roccamonfina@asmepec.it",
                   "address": "Via Municipio N. 8",
                   "zipCode": "81035",
                   "geographicTaxonomies": [
                       {
                           "code": "ITA",
                           "desc": "ITALIA"
                       }
                   ],
                   "imported": false
               },
               "billing": {
                   "vatNumber": "83001010616",
                   "recipientCode": "UFO5LL",
                   "publicServices": true
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
                   "institutionType": "PA",
                   "taxCode": "83001010616",
                   "origin": "IPA",
                   "originId": "AL7RYI5",
                   "subunitCode": "AL7RYI5",
                   "subunitType": "AOO",
                   "city": "Roccamonfina",
                   "country": "IT",
                   "county": "CE",
                   "description": "Comune di Roccamonfina",
                   "digitalAddress": "protocollo.roccamonfina@asmepec.it",
                   "address": "Via Municipio N. 8",
                   "zipCode": "81035",
                   "geographicTaxonomies": [
                       {
                           "code": "ITA",
                           "desc": "ITALIA"
                       }
                   ],
                   "imported": false
               },
               "billing": {
                   "vatNumber": "83001010616",
                   "recipientCode": "UFO5LL",
                   "publicServices": true
               }
           }
        """
    Then the response status code should be 200

  Scenario: Successfully perform onboarding request for UO
    Given I have a request object
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
                   "institutionType": "PA",
                   "taxCode": "83001010616",
                   "origin": "IPA",
                   "originId": "UFO5LL",
                   "subunitCode": "UFO5LL",
                   "subunitType": "UO",
                   "city": "Roccamonfina",
                   "country": "IT",
                   "county": "CE",
                   "description": "Comune di Roccamonfina",
                   "digitalAddress": "protocollo.roccamonfina@asmepec.it",
                   "address": "Via Municipio N. 8",
                   "zipCode": "81035",
                   "geographicTaxonomies": [
                       {
                           "code": "ITA",
                           "desc": "ITALIA"
                       }
                   ],
                   "imported": false
               },
               "billing": {
                   "vatNumber": "83001010616",
                   "recipientCode": "UFO5LL",
                   "publicServices": true
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
                   "institutionType": "PA",
                   "taxCode": "83001010616",
                   "origin": "IPA",
                   "originId": "UFO5LL",
                   "subunitCode": "UFO5LL",
                   "subunitType": "UO",
                   "city": "Roccamonfina",
                   "country": "IT",
                   "county": "CE",
                   "description": "Comune di Roccamonfina",
                   "digitalAddress": "protocollo.roccamonfina@asmepec.it",
                   "address": "Via Municipio N. 8",
                   "zipCode": "81035",
                   "geographicTaxonomies": [
                       {
                           "code": "ITA",
                           "desc": "ITALIA"
                       }
                   ],
                   "imported": false
               },
               "billing": {
                   "vatNumber": "83001010616",
                   "recipientCode": "UFO5LL",
                   "publicServices": true
               }
           }
        """
    Then the response status code should be 200