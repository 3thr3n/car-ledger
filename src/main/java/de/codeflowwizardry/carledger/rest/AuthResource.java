package de.codeflowwizardry.carledger.rest;

import java.net.URI;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;

import io.quarkus.oidc.OidcSession;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("auth")
public class AuthResource
{
	private final OidcSession session;

	@ConfigProperty(name = "redirect.to.logout")
	String redirectToLogout;

	@ConfigProperty(name = "redirect.to.error")
	String redirectToError;

	@Inject
	public AuthResource(OidcSession session)
	{
		this.session = session;
	}

	@GET
	@Path("login")
	@Operation(operationId = "login", description = "Here should the browser redirect, when 'login' is pressed")
	public Response login()
	{
		return Response.ok().build();
	}

	@GET
	@Path("logout")
	@Operation(operationId = "logout", description = "Logout current user")
	public Uni<Response> logout()
	{
		return session.logout().onItem().transform((x) -> Response.status(302).location(URI.create(redirectToLogout)).build());
	}

    @GET
    @Path("logout-callback")
    @Operation(operationId = "callback", description = "This only for redirect purposes of oauth!")
    public Response logoutCallback()
    {
        return Response.status(302).location(URI.create(redirectToLogout)).build();
    }

	@GET
	@Path("callback")
	@Operation(operationId = "callback", description = "This only for redirect purposes of oauth!")
	public Response callback()
	{
		return Response.status(302).location(URI.create(redirectToLogout)).build();
	}
}
