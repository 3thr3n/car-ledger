package de.codeflowwizardry.carledger.rest;

import java.io.IOException;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

/**
 * PostgreSQL can't store a NUL byte in a text: a request with one is rejected
 * with 400 instead of failing in the database with 500, in the JSON and in the
 * URL (path and query, e.g. a search).
 */
@ApplicationScoped
public class NulBytes
{
	/** before the body is read, e.g. of an upload */
	static final int ORDER = -900;

	void install(@Observes Router router)
	{
		router.route("/api/*").order(ORDER).handler(NulBytes::handle);
	}

	static void handle(RoutingContext context)
	{
		if (inUri(context.request().uri()))
		{
			context.response().setStatusCode(400).end("The request must not contain a NUL character");
			return;
		}
		context.next();
	}

	/**
	 * @param uri
	 *            as sent, path and query still encoded
	 */
	static boolean inUri(String uri)
	{
		return uri != null && (uri.indexOf('\0') >= 0 || uri.toLowerCase(Locale.ROOT).contains("%00"));
	}

	/**
	 * Strings of the JSON bodies with a NUL fail like a value of a wrong type,
	 * with 400
	 */
	@Singleton
	static class Strings implements ObjectMapperCustomizer
	{
		@Override
		public void customize(ObjectMapper mapper)
		{
			mapper.registerModule(new SimpleModule("NulBytes").addDeserializer(String.class, new Deserializer()));
		}
	}

	static class Deserializer extends StringDeserializer
	{
		@Override
		public String deserialize(JsonParser parser, DeserializationContext context) throws IOException
		{
			String value = super.deserialize(parser, context);
			if (value != null && value.indexOf('\0') >= 0)
			{
				throw context.weirdStringException(value, String.class, "a NUL character is not allowed");
			}
			return value;
		}
	}
}
