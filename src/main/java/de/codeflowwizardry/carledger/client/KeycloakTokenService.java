package de.codeflowwizardry.carledger.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "keycloak")
@ApplicationScoped
public interface KeycloakTokenService {

    @POST
    @Path("/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    KeycloakTokenResponse getToken(@FormParam("grant_type") String grantType,
                                        @FormParam("client_id") String clientId,
                                        @FormParam("client_secret") String clientSecret,
                                        @FormParam("username") String username,
                                        @FormParam("password") String password);
}
