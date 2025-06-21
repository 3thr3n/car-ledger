package de.codeflowwizardry.carledger.rest.filters;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
public class SessionFilter implements ContainerRequestFilter {

    private final static Logger LOG = LoggerFactory.getLogger(SessionFilter.class);

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login",
            "/auth/logout",
            "/public"
    );

    @ConfigProperty(name = "test.mode.enabled", defaultValue = "false")
    boolean testMode;

    @Override
    public void filter(ContainerRequestContext ctx) {
        if (testMode) return;

        LOG.debug("Filtering request");
        String path = ctx.getUriInfo().getPath();

        if (isPublicPath(path)) {
            LOG.debug("Skip path: {}", path);
            return; // Skip auth check for public endpoints
        }
        LOG.debug("Not skipping path: {}", path);

        Cookie cookie = ctx.getCookies().get("SESSION_ID");
        if (cookie == null) {
            ctx.abortWith(Response.status(UNAUTHORIZED).build());
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.contains(path);
    }
}