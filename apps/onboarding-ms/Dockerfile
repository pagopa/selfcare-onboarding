# syntax=docker/dockerfile:1.6
FROM maven:3-eclipse-temurin-17 AS builder

WORKDIR /src
COPY ./pom.xml .

WORKDIR /src/libs
COPY ./libs/pom.xml .

WORKDIR /src/libs/onboarding-sdk
COPY ./libs/onboarding-sdk/ .

WORKDIR /src/apps
COPY ./apps/pom.xml .

WORKDIR /src/apps/onboarding-ms
COPY ./apps/onboarding-ms/pom.xml .
COPY ./apps/onboarding-ms/src/main/ ./src/main/

WORKDIR /src
RUN mvn --projects :onboarding-ms --also-make clean package -DskipTests

FROM openjdk:17-jdk AS runtime

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

WORKDIR /app

COPY --from=builder /src/apps/onboarding-ms/target/quarkus-app/lib/ ./lib/
COPY --from=builder /src/apps/onboarding-ms/target/quarkus-app/*.jar ./
COPY --from=builder /src/apps/onboarding-ms/target/quarkus-app/app/ ./app/
COPY --from=builder /src/apps/onboarding-ms/target/quarkus-app/quarkus/ ./quarkus/

EXPOSE 8080
USER 1001

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTIONS -jar /app/quarkus-run.jar"]
