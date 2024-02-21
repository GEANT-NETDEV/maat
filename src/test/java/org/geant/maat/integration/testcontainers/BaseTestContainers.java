package org.geant.maat.integration.testcontainers;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Consumer;

@SpringBootTest
@DirtiesContext
@Testcontainers
public class BaseTestContainers {

    static int hostPort = 27017;
    static int containerExposedPort = 27017;
    static Consumer<CreateContainerCmd> cmd = e -> e.withPortBindings(new PortBinding(Ports.Binding.bindPort(hostPort), new ExposedPort(containerExposedPort)));

    @Container
    public static GenericContainer mongo = new GenericContainer(DockerImageName.parse("mongo:latest"))
            .withExposedPorts(containerExposedPort)
            .withCreateContainerCmdModifier(cmd)
            .withEnv("MONGO_INITDB_ROOT_USERNAME", "admin")
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", "abc123")
            .waitingFor(Wait.forHttp("/"));

    //@BeforeAll
    //public static void beforeAll() {mongo.start();}

}
