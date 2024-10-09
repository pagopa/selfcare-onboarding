# Selfcare Onboarding

This repo structure and build monorepo with Apache Maven for selfcare onboarding domain.

Applications under apps/ depend on shared code under libs/. test-coverage/ is used to assess the test coverage of the entire project.


```
.

├── apps
│   ├── onboarding-functions
│   └── onboarding-ms
└── libs
    ├── onboarding-sdk-pom
    ├── onboarding-sdk-common
    ├── onboarding-sdk-azure-storage
    ├── onboarding-sdk-product
    ├── onboarding-sdk-crypto
└── test-coverage
```

Look at single README module for more information.

## Infrastructure

The [`.infra/`] sub folder contains terraform files for deploying infrastructure such as container apps or functions in Azure.


## Continous integration

The [`.github/`] sub folder contains a self-contained ci-stack for building the monorepo with Github Actions.

## Usage

```shell script
mvn clean package install
```

## Maven basic actions for monorep

Maven is really not a monorepo-*native* build tool (e.g. lacks
trustworthy incremental builds, can only build java code natively, is recursive and
struggles with partial repo checkouts) but can be made good use of with some tricks
and usage of a couple of lesser known command line switches.

| Action                                                                                             |  in working directory  | with Maven                                                                            |
|:---------------------------------------------------------------------------------------------------|:----------------------:|:--------------------------------------------------------------------------------------|
| Build the world                                                                                    |          `.`           | `mvn clean package -DskipTests`                                                       |
| Run `onboarding-ms`                                                                                |          `.`           | `java -jar apps/onboarding-ms/target/onboarding-ms-1.0.0-SNAPSHOT.jar`                |
| Build and test the world                                                                           |     `.`                | `mvn clean package`                                                                   |
| Build the world                                                                                    | `./apps/onboarding-ms` | `mvn --file ../.. clean package -DskipTests`                                          |
| Build `onboarding-ms` and its dependencies                                                         |          `.`           | `mvn --projects :onboarding-ms --also-make clean package -DskipTests`                 |
| Build `onboarding-ms` and its dependencies                                                         | `./apps/onboarding-ms` | `mvn --file ../.. --projects :onboarding-ms --also-make clean package -DskipTests`    |
| Build `onboarding-sdk` and its dependents (aka. reverse dependencies or *rdeps* in Bazel parlance) |          `.`           | `mvn --projects :onboarding-sdk-pom --also-make-dependents clean package -DskipTests` |
| Print dependencies of `onboarding-sdk`                                                             | `./apps/onboarding-ms` | `mvn dependency:list`                                                                 |
| Change version  of `onboarding-sdk`                                                             | `.` | `mvn versions:set -DnewVersion=0.2.2 --projects :onboarding-sdk-pom  `                |
| Persist version  of `onboarding-sdk`                                                             | `.` | `mvn versions:commit   `                                                              |

-----

## Java formatter installation (Intellij)

1. Open plugins window (CTRL+Shift+A or CMD+Shift+A) and type "plugins"
2. Click on browse repositories.
3. Search for google-java-format and install it
4. Restart the IDE.

To enable the plugin executing the action (CTRL+Shift+A or CMD+Shift+A) ant type: Reformat with google-java-format

Enable auto-format on save:

1. File -> Settings -> Tools or CTRL+Alt+S/CMD+Alt+S -> Tools
2. Click on "Action on Save"
3. Enable: "Reformat Code"