package de.codeflowwizardry.carledger.tracker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
 * Between two points the road has a vertex in the middle, around 10.25° it
 * bends {@link #EAST} there. {@link #down} answers 503.
 */
public class FakeValhalla implements QuarkusTestResourceLifecycleManager
{
	static final double NORTH = 0.0001;
	/** of the vertex between two points around 10.25°, about 3.6 m */
	static final double EAST = 0.00005;
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
			ArrayNode edges = answer.putArray("edges");
			List<Double> road = new ArrayList<>();
			for (int i = 0; i < shape.size(); i++)
			{
				JsonNode point = shape.get(i);
				double lat = point.path("lat").asDouble() + NORTH;
				double lon = point.path("lon").asDouble();
				if (lat >= 60)
				{
					status = 400;
				}
				matched.addObject()
						.put("lat", lat)
						.put("lon", lon)
						.put("type", "matched")
						.put("edge_index", i)
						.put("distance_from_trace_point", lon > 10.5 ? 100 : 11);
				// each point at the start of its edge, with a vertex to the next one
				road.add(lat);
				road.add(lon);
				if (i + 1 < shape.size())
				{
					JsonNode next = shape.get(i + 1);
					road.add((lat + next.path("lat").asDouble() + NORTH) / 2);
					road.add((lon + next.path("lon").asDouble()) / 2 + (bend(lon) ? EAST : 0));
				}
				edges.addObject().put("begin_shape_index", 2 * i).put("end_shape_index",
						Math.min(2 * i + 1, 2 * shape.size() - 2));
			}
			answer.put("shape", Polyline.encode(road.stream().mapToDouble(Double::doubleValue).toArray()));
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

	static boolean bend(double lon)
	{
		return lon > 10.2 && lon < 10.3;
	}

	@Override
	public void stop()
	{
		server.stop(0);
	}
}
