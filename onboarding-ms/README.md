# Microservice Onboarding

Repository that contains backend services synch for selfcare onboarding.

## Configuration Properties

Before running you must set these properties as environment variables.


| **Property**                                  | **Environment Variable**                 | **Default** | **Required** |
|-----------------------------------------------|------------------------------------------|-------------|:------------:|
| quarkus.mongodb.connection-string<br/>        | MONGODB_CONNECTION_URI                   |             |     yes      |
| mp.jwt.verify.publickey<br/>                  | JWT_TOKEN_PUBLIC_KEY                     |             |     yes      |
| quarkus.rest-client."**.UserApi".api-key<br/> | USER_REGISTRY_API_KEY                    |             |     yes      |
| quarkus.rest-client."**.UserApi".url<br/>     | USER_REGISTRY_URL                        |             |     yes      |
| quarkus.rest-client."**.ProductApi".url<br/>  | MS_PRODUCT_URL                           |             |     yes      |
| quarkus.rest-client."**.ProductApi".url<br/>  | ONBOARDING_ALLOWED_INSTITUTIONS_PRODUCTS |             |      no      |

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

For some endpoints 

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Related Guides


### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

### OpenAPI Generator

Rest client are generated using a quarkus' extension.

[Related guide section...](hhttps://github.com/quarkiverse/quarkus-openapi-generator)
