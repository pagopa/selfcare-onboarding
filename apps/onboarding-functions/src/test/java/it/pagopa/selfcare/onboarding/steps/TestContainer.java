package it.pagopa.selfcare.onboarding.steps;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.net.URL;
import java.time.Duration;

@Slf4j
@ApplicationScoped
public class TestContainer {

    void setupServices() {
        ComposeContainer composeContainer;
        File config;

        try {
            URL resource = getClass().getClassLoader().getResource("docker-compose.yml");
            if (resource == null) {
                throw new IllegalArgumentException("File of docker-compose not found!");
            } else {
                config = new File(resource.toURI());
            }

            composeContainer = new ComposeContainer(config).withLocalCompose(true);
                   /*
                    .withExposedService("mongo-db", 27017, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))
                    .withExposedService("azurite", 10010, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))
                    .withExposedService("azurite", 10011, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))
                    .withExposedService("azurite", 10012, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))
                    .withExposedService("mock-server", 1080, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))
                    .withExposedService("onboarding-fn", 8090,Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

                    */
            composeContainer.start();
            Runtime.getRuntime().addShutdownHook(new Thread(composeContainer::stop));
        } catch (Exception e) {
            log.error("Exception", e);
        }
    }
}
