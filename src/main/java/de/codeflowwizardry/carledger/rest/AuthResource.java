package de.codeflowwizardry.carledger.rest;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;

import io.quarkus.oidc.OidcSession;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("auth")
public class AuthResource
{
	private final OidcSession session;

	@ConfigProperty(name = "redirect.to.logout")
	String redirectToLogout;

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
		return Response.status(302).location(URI.create(redirectToLogout)).build();
	}

	@GET
	@Path("logout")
	@Operation(operationId = "logout", description = "Logout current user")
	public Uni<Response> logout()
	{
		return session.logout().onItem()
				.transform((x) -> Response.status(302).location(URI.create(redirectToLogout)).build());
	}

	@GET
	@Path("logout-callback")
	@Operation(operationId = "logout-callback", description = "This only for redirect purposes of oauth!")
	public Response logoutCallback()
	{
		return Response.status(302).location(URI.create(redirectToLogout)).build();
	}

	@GET
	@Path("error")
	@Operation(operationId = "loginError", description = "The provider redirects here when the login failed, shows the error in the frontend")
	public Response error(@QueryParam("error") String error, @QueryParam("error_description") String description)
	{
		String reason = description != null && !description.isBlank() ? description : error;
		String errorPage = (redirectToLogout.endsWith("/") ? redirectToLogout : redirectToLogout + "/") + "error";
		if (reason != null && !reason.isBlank())
		{
			errorPage += "?reason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8);
		}
		return Response.status(302).location(URI.create(errorPage)).build();
	}

	@GET
	@Path("callback")
	@Operation(operationId = "callback", description = "This only for redirect purposes of oauth!")
	public Response callback()
	{
		return Response.status(302).location(URI.create(redirectToLogout + "?followRedirect")).build();
	}
}
