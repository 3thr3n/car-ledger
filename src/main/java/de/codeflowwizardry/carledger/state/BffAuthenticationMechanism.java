package de.codeflowwizardry.carledger.state;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashSet;

@ApplicationScoped
@Priority(100) // Should run early
public class BffAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    SessionContext sessionContext;

    @Inject
    CurrentIdentityAssociation identityAssociation;

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager idpManager) {
        SessionUser user = sessionContext.get();

        if (user != null) {
            SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                    .setPrincipal(user::username)
                    .addAttribute("name", user.user())
                    .addAttribute("email", user.email())
                    .addRoles(new HashSet<>(user.roles()))
                    .build();

            identityAssociation.setIdentity(identity);

            return Uni.createFrom().item(identity);
        }
        return Uni.createFrom().item(QuarkusSecurityIdentity.builder().setAnonymous(true).build());
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        // No challenge needed since auth happens earlier
        return Uni.createFrom().nothing();
    }
}
