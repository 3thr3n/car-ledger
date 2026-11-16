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
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

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
		return shape("/valhalla/shape.json");
	}

	private List<Pending> shape(String resource) throws IOException
	{
		List<Pending> points = new ArrayList<>();
		try (InputStream in = getClass().getResourceAsStream(resource))
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

	/**
	 * The hairpins of the Kesselberg (B11) with a point every 25 m, of the tiles
	 * of Germany, Austria and Switzerland: the line along the vertices follows the
	 * road, the one from point to point cuts the curves.
	 */
	@Test
	void shouldFollowTheRoadAroundTheHairpins() throws Exception
	{
		byte[] body;
		try (InputStream in = getClass().getResourceAsStream("/valhalla/kesselberg-trace_attributes.json"))
		{
			body = in.readAllBytes();
		}
		answer.set(body);

		List<ValhallaClient.Snapped> snapped = client.match(shape("/valhalla/kesselberg-shape.json"));

		assertEquals(60, snapped.size());
		assertNull(snapped.getFirst().road());
		// Valhalla goes 413 m between the points 3 and 4, 26 m apart in 2 s: a loop of
		// the matching, the straight line stays
		assertNull(snapped.get(4).road());
		// the others on a straight piece, without a vertex between them
		assertEquals(53, snapped.stream().filter(point -> point.road() != null).count());

		List<TrackPoint> withRoad = new ArrayList<>();
		List<TrackPoint> pointToPoint = new ArrayList<>();
		for (ValhallaClient.Snapped point : snapped)
		{
			double[] vertices = Polyline.decode(point.road());
			for (int k = 0; k < vertices.length; k += 2)
			{
				withRoad.add(new TrackPoint(null, vertices[k], vertices[k + 1], null));
			}
			withRoad.add(new TrackPoint(null, point.lat(), point.lon(), null));
			pointToPoint.add(new TrackPoint(null, point.lat(), point.lon(), null));
		}

		// the road of Valhalla after the loop up to the last point
		JsonNode valhalla = mapper.readTree(body);
		double[] shape = Polyline.decode(valhalla.path("shape").asText());
		int first = segment(valhalla, shape, 4, snapped.get(4));
		int last = segment(valhalla, shape, snapped.size() - 1, snapped.getLast());
		double alongTheRoad = 0;
		double cut = 0;
		for (int k = first + 1; k <= last; k++)
		{
			TrackPoint vertex = new TrackPoint(null, shape[k * 2], shape[k * 2 + 1], null);
			alongTheRoad = Math.max(alongTheRoad, distance(vertex, withRoad));
			cut = Math.max(cut, distance(vertex, pointToPoint));
		}
		assertTrue(alongTheRoad < 0.5, "along the road: " + alongTheRoad);
		assertTrue(cut > 3, "point to point: " + cut);
		// a few bytes per point
		int bytes = snapped.stream().mapToInt(point -> point.road() == null ? 0 : point.road().length()).sum();
		assertTrue(bytes < 60 * 25, "bytes: " + bytes);
	}

	/** of the shape, the piece the point lies on */
	private static int segment(JsonNode valhalla, double[] shape, int index, ValhallaClient.Snapped point)
	{
		JsonNode edge = valhalla.path("edges")
				.path(valhalla.path("matched_points").get(index).path("edge_index").asInt());
		return ValhallaClient.segment(shape, edge, point.lat(), point.lon());
	}

	/** meters from the vertex to the line */
	private static double distance(TrackPoint vertex, List<TrackPoint> line)
	{
		double nearest = Double.MAX_VALUE;
		for (int i = 1; i < line.size(); i++)
		{
			nearest = Math.min(nearest, TrackSimplifier.distanceToSegment(vertex, line.get(i - 1), line.get(i)));
		}
		return nearest;
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

	@Test
	void shouldWaitOnAWrongUrl()
	{
		// the answer of Valhalla to an unknown path, the points must not lose their
		// roads
		status.set(404);
		answer.set("{\"error_code\":106,\"error\":\"Try any of: '/locate' '/route'\"}".getBytes());

		IOException e = assertThrows(IOException.class, () -> client.match(shape()));
		assertEquals("Valhalla answered 404: Try any of: '/locate' '/route'", e.getMessage());
	}
}
