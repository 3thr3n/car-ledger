package de.codeflowwizardry.carledger.rest;

import java.net.URI;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;

import io.quarkus.oidc.OidcSession;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.RedirectionException;
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
	public Uni<Void> logout()
	{
		return session.logout();
	}

	@GET
	@Path("callback")
	@Operation(operationId = "callback", description = "This only for redirect purposes of oauth!")
	public Response callback()
	{
		throw new RedirectionException(302, URI.create(redirectToLogout));
	}

	@GET
	@Path("error")
	@Operation(operationId = "error", description = "This redirects to a frontend error page!")
	public Response error()
	{
		throw new RedirectionException(302, URI.create(redirectToError));
	}
}
