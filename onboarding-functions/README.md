# Onboarding Functions

Repository that contains Azure functions designed for onboarding asynchronous flow activities.
These functions handle all asynchronous activities related to preparing and completing the onboarding process. Indeed, they are activated by the onboarding microservice upon receiving an onboarding request.

1. StartOnboardingOrchestration:

It is triggered by http request at GET or POST `/api/StartOnboardingOrchestration?onboardingId={onboardingId}` where onboardingId is a reference to onboarding which you want to process.

## Configuration Properties

Before running you must set these properties as environment variables.


| **Property**                                       | **Environment Variable**     | **Default** | **Required** |
|----------------------------------------------------|------------------------------|-------------|:------------:|
| quarkus.mongodb.connection-string<br/>             | MONGODB_CONNECTION_URI       |             |     yes      |
| quarkus.azure-functions.app-name<br/>              | AZURE_APP_NAME               |             |      no      |
| quarkus.azure-functions.subscription-id<br/>       | AZURE_SUBSCRIPTION_ID        |             |      no      |
| quarkus.azure-functions.resource-group<br/>        | AZURE_RESOURCE_GROUP         |             |      no      |
| quarkus.azure-functions.app-insights-key<br/>      | AZURE_APP_INSIGHTS_KEY       |             |      no      |
| quarkus.azure-functions.app-service-plan-name<br/> | AZURE_APP_SERVICE_PLAN_NAME  |             |      no      |

## Packaging

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `onboarding-functions-1.0.0-SNAPSHOT.jar` file in the `target/` directory.

## Start application

```shell script
./mvnw quarkus:run
```

If you want enable debugging you must add -DenableDebug

```shell script
./mvnw quarkus:run -DenableDebug
```
You can follow this guide for debugging application in IntelliJ https://www.jetbrains.com/help/idea/tutorial-remote-debug.html

## Related Guides

- Azure Functions ([guide](https://quarkus.io/guides/azure-functions)): Write Microsoft Azure functions


