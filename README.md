# Selfcare Onboarding

Repository that contains backend services for selfcare onboarding. It is a monorepo for onboarding domain that contains:

- `onboarding-functions`: functions that handle all asynchronous activities related to preparing and completing the onboarding process. Indeed, they are activated by the onboarding microservice upon receiving an onboarding request
- `onboarding-ms`: microservice that implements CRUD operations for the 'onboarding' object and the business logic for the onboarding phase. During the onboarding process
- `onboarding-sdk`: Java utility classes that simplify the work of developers about onboarding activity

Look at single README module for more information.
