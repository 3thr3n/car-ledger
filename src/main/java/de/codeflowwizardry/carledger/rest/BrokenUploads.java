package de.codeflowwizardry.carledger.rest;

import java.io.IOException;

import org.jboss.resteasy.reactive.server.core.multipart.MultipartParser.MalformedMessageException;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * A broken upload is the fault of the request: 400 instead of 500. It fails
 * while the body is read, before the exception mappers of the resources:
 * malformed parts, or a body without its end (an IOException).
 */
@ApplicationScoped
public class BrokenUploads
{
	void install(@Observes Router router)
	{
		router.route("/api/*").failureHandler(BrokenUploads::handle);
	}

	static void handle(RoutingContext context)
	{
		if (isBroken(context) && !context.response().headWritten())
		{
			context.response().setStatusCode(400).end("The upload is not a valid multipart body");
			return;
		}
		context.next();
	}

	private static boolean isBroken(RoutingContext context)
	{
		String type = context.request().getHeader("Content-Type");
		return type != null && type.toLowerCase().startsWith("multipart/")
				&& (context.failure() instanceof MalformedMessageException || context.failure() instanceof IOException);
	}
}
