# Test Coverage

It is a submodule designed to assess the test coverage of the entire project. It contains various configurations to evaluate the coverage of individual modules. Specifically, it leverages the Maven plugin, Jacoco, using the 'report-aggregate' goal to aggregate reports from the individual modules and provide them to Sonarqube for test coverage and code quality verification. It's essential for each module to have the same plugin or library that generates the jacoco.xml file, summarizing the test coverage for that specific module.

## Usage

To run coverage on a specific module, dedicated profiles have been configured. For example, to execute it on the `apps/onboarding-ms module`, you can use the following command:

```shell script
mvn --projects :test-coverage --also-make verify -Ponboarding-ms,report
```

* **report** is used for generating reports.
* **onboarding-ms** is used to perform the scan on onboarding-ms module.

Make sure you have the necessary configurations and dependencies in place for accurate test coverage analysis.

## Sonarcloud

To enable performing the scan on Sonarcloud you can add profile `coverage` and some other information:

```shell script
mvn --projects :test-coverage --also-make verify -Ponboarding-ms,report,coverage -Dsonar.organization=xxx -Dsonar.projectKey=yyy -Dsonar.token=zzzz -Dsonar.pullrequest.key=123
```
