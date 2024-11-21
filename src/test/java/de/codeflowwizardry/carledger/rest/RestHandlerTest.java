package de.codeflowwizardry.carledger.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class RestHandlerTest
{
	@Test
	void shouldReturnExceptionResponse()
	{
		// given
		RestHandler restHandler = new RestHandler(null);

		// when
		WebApplicationException wae = new InternalServerErrorException("Why??");
		Response response = restHandler.handleExceptions(wae);

		// then
		assertEquals(500, response.getStatus());
	}

	@Test
	void shouldHandle404ForApi()
	{
		// given
		UriInfo uriInfo = Mockito.mock(UriInfo.class);
		Mockito.when(uriInfo.getRequestUri()).thenReturn(URI.create("/api/meep"));

		RestHandler restHandler = new RestHandler(uriInfo);

		// when
		Response handleWeb404 = restHandler.handleWeb404(new NotFoundException());

		// then
		assertEquals(404, handleWeb404.getStatus());
	}

	@Test
	void shouldHandle404ForRest()
	{
		// given
		UriInfo uriInfo = Mockito.mock(UriInfo.class);
		Mockito.when(uriInfo.getRequestUri()).thenReturn(URI.create("/asdff"));

		RestHandler restHandler = new RestHandler(uriInfo);

		// when
		Response handleWeb404 = restHandler.handleWeb404(new NotFoundException());

		// then
		assertEquals(303, handleWeb404.getStatus());
		assertEquals(URI.create("/"), handleWeb404.getLocation());
	}
}
