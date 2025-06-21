package de.codeflowwizardry.carledger;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class KeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private final static Integer KEYCLOAK_PORT = 8080;

    // optional depending on setup
    private final DockerComposeContainer<?> environment;

    public KeycloakTestResource() {
        environment = new DockerComposeContainer<>(new File("keycloak-compose.yml"))
                .withExposedService("keycloak", KEYCLOAK_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));
    }

    @Override
    public Map<String, String> start() {
        environment.start();

        Integer keycloakPort = environment.getServicePort("keycloak", KEYCLOAK_PORT);

        Map<String, String> config = new HashMap<>();
        config.put("keycloak.realm.url", "http://localhost:" + keycloakPort + "/realms/quarkus"); // Or any relevant property
        return config;
    }

    @Override
    public void stop() {
        environment.stop();
    }
}