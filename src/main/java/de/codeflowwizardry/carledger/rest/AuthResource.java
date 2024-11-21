package de.codeflowwizardry.carledger.rest;

import java.net.URI;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.RedirectionException;
import jakarta.ws.rs.core.Response;

@Path("auth")
public class AuthResource
{
	@ConfigProperty(name = "redirect.to")
	String redirectTo;

	@GET
	@Path("login")
	@Operation(operationId = "login", description = "Here should the browser redirect, when 'login' is pressed")
	public String login()
	{
		return "Just a placeholder"; // TODO replace this stub to something useful
	}

	@GET
	@Path("callback")
	@Operation(operationId = "callback", description = "This only for redirect purposes of oauth!")
	public Response callback()
	{
		throw new RedirectionException(302, URI.create(redirectTo));
	}
}
