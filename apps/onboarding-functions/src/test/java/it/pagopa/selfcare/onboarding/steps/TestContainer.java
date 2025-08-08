package it.pagopa.selfcare.onboarding.steps;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Slf4j
@ApplicationScoped
public class TestContainer {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    void setupServices() {
        ComposeContainer compose = new ComposeContainer(new File("src/test/resources/docker-compose.yml"))
                .withLocalCompose(true);

        List<ServicePort> services = Arrays.asList(
                new ServicePort("mongo-db", 27017),
                new ServicePort("azurite", 1000),
                new ServicePort("azurite", 10001),
                new ServicePort("azurite", 10002),
                new ServicePort("mock-server", 1080),
                new ServicePort("institution-ms", 8082),
                new ServicePort("user-ms", 8087),
                new ServicePort("onboarding-fn", 8090)
        );

        services.forEach(sp ->
                compose.withExposedService(
                        sp.name(),
                        sp.port(),
                        Wait.forListeningPort()//.withStartupTimeout(TIMEOUT)
                )
        );

        compose.start();
    }

    private record ServicePort(String name, int port) {
    }
}
