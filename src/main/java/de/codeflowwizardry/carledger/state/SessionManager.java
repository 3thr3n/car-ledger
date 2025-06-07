package de.codeflowwizardry.carledger.state;

import de.codeflowwizardry.carledger.client.KeycloakTokenResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SessionManager {
    private final Map<String, KeycloakTokenResponse> sessions = new ConcurrentHashMap<>();

    public void store(String sessionId, KeycloakTokenResponse token) {
        sessions.put(sessionId, token);
    }

    public KeycloakTokenResponse get(String sessionId) {
        return sessions.get(sessionId);
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }
}
