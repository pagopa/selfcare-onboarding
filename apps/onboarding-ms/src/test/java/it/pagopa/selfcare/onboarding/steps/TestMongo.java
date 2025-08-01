package it.pagopa.selfcare.onboarding.steps;

import java.io.IOException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

public class TestMongo {

    static Network network;

    static GenericContainer<?> mongo1;
    static GenericContainer<?> mongo2;
    static GenericContainer<?> mongo3;

    public void setUp() throws IOException, InterruptedException {
        network = Network.newNetwork();

        mongo1 =
                new GenericContainer<>(DockerImageName.parse("mongo:6.0"))
                        .withNetwork(network)
                        .withNetworkAliases("mongo1")
                        .withCommand("--replSet", "rs0", "--bind_ip_all")
                        .withExposedPorts(27017);

        mongo2 =
                new GenericContainer<>(DockerImageName.parse("mongo:6.0"))
                        .withNetwork(network)
                        .withNetworkAliases("mongo2")
                        .withCommand("--replSet", "rs0", "--bind_ip_all")
                        .withExposedPorts(27017);

        mongo3 =
                new GenericContainer<>(DockerImageName.parse("mongo:6.0"))
                        .withNetwork(network)
                        .withNetworkAliases("mongo3")
                        .withCommand("--replSet", "rs0", "--bind_ip_all")
                        .withExposedPorts(27017);

        mongo1.start();
        mongo2.start();
        mongo3.start();

        // Init replica set from mongo1
        String initiateCmd =
                """
                    rs.initiate({
                      _id: "rs0",
                      members: [
                        { _id: 0, host: "mongo1:27017" },
                        { _id: 1, host: "mongo2:27017" },
                        { _id: 2, host: "mongo3:27017" }
                      ]
                    })
                    """;

        mongo1.execInContainer("mongosh", "--eval", initiateCmd);

        try {
            Thread.sleep(5000); // wait for replica to settle
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}