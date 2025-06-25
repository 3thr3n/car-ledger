package de.codeflowwizardry.carledger.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@ApplicationScoped
@RegisterRestClient(configKey = "keycloak")
public interface KeycloakTokenService
{

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	KeycloakTokenResponse getToken(@FormParam("grant_type") String grantType,
			@FormParam("client_id") String clientId,
			@FormParam("client_secret") String clientSecret,
			@FormParam("username") String username,
			@FormParam("password") String password);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	KeycloakTokenResponse refreshToken(@FormParam("grant_type") String grantType,
			@FormParam("client_id") String clientId,
			@FormParam("client_secret") String clientSecret,
			@FormParam("refresh_token") String refreshToken);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	KeycloakTokenResponse processCode(@FormParam("grant_type") String grantType,
			@FormParam("code") String code,
			@FormParam("client_id") String clientId,
			@FormParam("client_secret") String clientSecret,
			@FormParam("redirect_uri") String refreshToken);
}
