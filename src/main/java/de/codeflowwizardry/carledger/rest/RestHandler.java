package de.codeflowwizardry.carledger.rest;

import java.net.URI;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class RestHandler
{
	private final static Logger LOG = LoggerFactory.getLogger(RestHandler.class);

	private final UriInfo uriInfo;

	@Inject
	public RestHandler(@Context UriInfo uriInfo)
	{
		this.uriInfo = uriInfo;
	}

	@ServerExceptionMapper
	public Response handleExceptions(WebApplicationException exception)
	{
		return exception.getResponse();
	}

	@ServerExceptionMapper
	public Response handleWeb404(NotFoundException nfe)
	{
		LOG.info("Handle 404");

		String path = uriInfo.getRequestUri().getPath();

		if (path.startsWith("/api"))
		{
			return nfe.getResponse();
		}

		return Response.seeOther(URI.create("/")).build();
	}
}
