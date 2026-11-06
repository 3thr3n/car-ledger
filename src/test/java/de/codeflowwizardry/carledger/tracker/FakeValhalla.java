package de.codeflowwizardry.carledger.tracker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Valhalla of the tests: every point lies on a road 0.0001° (11 m) north of it,
 * east of 10.5° 100 m away (a parking lot); a drive north of 60° has no road.
 * {@link #down} answers 503.
 */
public class FakeValhalla implements QuarkusTestResourceLifecycleManager
{
	static final double NORTH = 0.0001;
	static final AtomicBoolean down = new AtomicBoolean();
	static final AtomicInteger requests = new AtomicInteger();

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private HttpServer server;

	@Override
	public Map<String, String> start()
	{
		try
		{
			server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
		server.createContext("/trace_attributes", exchange -> {
			requests.incrementAndGet();
			JsonNode shape = MAPPER.readTree(exchange.getRequestBody()).path("shape");
			int status = 200;
			ObjectNode answer = MAPPER.createObjectNode();
			ArrayNode matched = answer.putArray("matched_points");
			for (JsonNode point : shape)
			{
				if (point.path("lat").asDouble() >= 60)
				{
					status = 400;
				}
				matched.addObject()
						.put("lat", point.path("lat").asDouble() + NORTH)
						.put("lon", point.path("lon").asDouble())
						.put("type", "matched")
						.put("distance_from_trace_point", point.path("lon").asDouble() > 10.5 ? 100 : 11);
			}
			if (down.get())
			{
				status = 503;
			}
			byte[] body = (status == 400
					? "{\"error_code\":444,\"error\":\"Map Match algorithm failed to find path\"}"
					: MAPPER.writeValueAsString(answer)).getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(status, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		return Map.of("carledger.map.matching.url", "http://localhost:" + server.getAddress().getPort(),
				// the tests run the matching themselves
				"carledger.map.matching.every", "off");
	}

	@Override
	public void stop()
	{
		server.stop(0);
	}
}
