package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import de.codeflowwizardry.carledger.data.repository.TrackPointRepository.Pending;

/**
 * The answer of a real Valhalla (3.9.1, Andorra) to six points of a drive, one
 * of them left out as an outlier
 */
class ValhallaClientTest
{
	private final ObjectMapper mapper = new ObjectMapper();
	private final AtomicInteger status = new AtomicInteger(200);
	private final AtomicReference<byte[]> answer = new AtomicReference<>();
	private final AtomicReference<JsonNode> request = new AtomicReference<>();
	private HttpServer server;
	private ValhallaClient client;

	@BeforeEach
	void setup() throws IOException
	{
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/trace_attributes", exchange -> {
			request.set(mapper.readTree(exchange.getRequestBody()));
			byte[] body = answer.get();
			exchange.sendResponseHeaders(status.get(), body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		String url = "http://localhost:" + server.getAddress().getPort();
		client = new ValhallaClient(new MapMatchingConfig() {
			@Override
			public Optional<String> url()
			{
				return Optional.of(url);
			}

			@Override
			public double maxDistance()
			{
				return 50;
			}

			@Override
			public Duration timeout()
			{
				return Duration.ofSeconds(5);
			}

			@Override
			public int maxPoints()
			{
				return 2000;
			}

			@Override
			public String every()
			{
				return "off";
			}
		}, mapper);
	}

	@AfterEach
	void stop()
	{
		server.stop(0);
	}

	private List<Pending> shape() throws IOException
	{
		List<Pending> points = new ArrayList<>();
		try (InputStream in = getClass().getResourceAsStream("/valhalla/shape.json"))
		{
			long id = 1;
			for (JsonNode point : mapper.readTree(in))
			{
				points.add(new Pending(id++, OffsetDateTime.parse("2026-10-30T16:00:00Z")
						.plusSeconds(point.path("time").asLong() - 1793380000), point.path("lat").asDouble(),
						point.path("lon").asDouble()));
			}
		}
		return points;
	}

	@Test
	void shouldSnapThePointsAndLeaveOutTheOutlier() throws Exception
	{
		try (InputStream in = getClass().getResourceAsStream("/valhalla/trace_attributes.json"))
		{
			answer.set(in.readAllBytes());
		}

		List<ValhallaClient.Snapped> snapped = client.match(shape());

		assertEquals(6, snapped.size());
		assertEquals(new ValhallaClient.Snapped(42.505238, 1.527556, 8.923185), snapped.getFirst());
		// interpolated is on the road as well
		assertEquals(2.746384, snapped.get(1).distance());
		assertNull(snapped.get(3));
		assertEquals("map_snap", request.get().path("shape_match").asText());
		assertEquals(6, request.get().path("shape").size());
		assertTrue(request.get().path("shape").get(0).has("time"));
	}

	@Test
	void shouldTellADriveWithoutARoad()
	{
		status.set(400);
		answer.set("{\"error_code\":444,\"error\":\"Map Match algorithm failed to find path\"}".getBytes());

		ValhallaClient.NoRoadException e = assertThrows(ValhallaClient.NoRoadException.class,
				() -> client.match(shape()));
		assertEquals("Map Match algorithm failed to find path", e.getMessage());
	}

	@Test
	void shouldFailWhenTheServiceFails()
	{
		status.set(503);
		answer.set(new byte[0]);

		assertThrows(IOException.class, () -> client.match(shape()));
	}
}
