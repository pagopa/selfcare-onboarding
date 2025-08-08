package it.pagopa.selfcare.onboarding.steps;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

@Slf4j
@ApplicationScoped
public class TestContainer {

    void setupServices() {
        ComposeContainer compose = new ComposeContainer(new File("src/test/resources/docker-compose.yml"))
                .withLocalCompose(true)
                .waitingFor("azure-cli", Wait.forLogMessage(".*BLOBSTORAGE INITIALIZED.*\\n", 1));

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
                        sp.port()
                )
        );

        compose.start();

        waitForPort(compose, "mongo-db", 27017);
        waitForPort(compose, "azurite", 1000);
        waitForPort(compose, "azurite", 10001);
        waitForPort(compose, "azurite", 10002);
        waitForPort(compose, "mock-server", 1080);

        waitForPort(compose, "institution-ms", 8082);
        waitForPort(compose, "user-ms", 8087);
        waitForPort(compose, "onboarding-fn", 8090);
    }

    private record ServicePort(String name, int port) {
    }

    private void waitForPort(ComposeContainer compose, String serviceName, int port) {
        String host = compose.getServiceHost(serviceName, port);
        Integer mappedPort = compose.getServicePort(serviceName, port);

        int maxRetries = 30;
        int retryDelayMillis = 1000;

        for (int i = 0; i < maxRetries; i++) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, mappedPort), retryDelayMillis);
                log.info("%s:%d is ready%n", serviceName, port);
                return;
            } catch (IOException e) {
                log.info("Waiting for %s:%d...%n", serviceName, port);
                try {
                    Thread.sleep(retryDelayMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for service " + serviceName);
                }
            }
        }

        throw new RuntimeException("Timeout waiting for service " + serviceName + " on port " + port);
    }

}
