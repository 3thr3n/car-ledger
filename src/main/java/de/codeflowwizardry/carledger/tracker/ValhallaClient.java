package de.codeflowwizardry.carledger.tracker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.codeflowwizardry.carledger.data.repository.TrackPointRepository.Pending;
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Snaps the points of a drive to the roads with <code>trace_attributes</code>
 * of a Valhalla service (<code>map_snap</code>: a hidden Markov model over the
 * roads near the points)
 */
@ApplicationScoped
public class ValhallaClient
{
	private final MapMatchingConfig config;
	private final ObjectMapper mapper;
	private final HttpClient client;

	@Inject
	public ValhallaClient(MapMatchingConfig config, ObjectMapper mapper)
	{
		this(config, mapper, HttpClient.newBuilder().connectTimeout(config.timeout()).build());
	}

	ValhallaClient(MapMatchingConfig config, ObjectMapper mapper, HttpClient client)
	{
		this.config = config;
		this.mapper = mapper;
		this.client = client;
	}

	/**
	 * On a curve the points are tens of meters apart: a road longer than this
	 * times the straight line, plus {@link #DETOUR_METERS}, is a jump of the
	 * matching (e.g. between two parts it couldn't connect), not the road
	 */
	static final double DETOUR_FACTOR = 3;
	static final double DETOUR_METERS = 50;
	/** faster along the road between two points, m/s, is a jump as well */
	static final double MAX_ROAD_SPEED = 75;
	/** a vertex this close to a point is the point, meters */
	static final double SAME_METERS = 0.2;

	/**
	 * A point on the road
	 *
	 * @param distance
	 *            from the position of the GPS, meters
	 * @param road
	 *            the vertices of the road from the point before to this one, an
	 *            encoded {@link Polyline}; <code>null</code> on a straight piece,
	 *            for the first point and after one left out
	 */
	public record Snapped(double lat, double lon, double distance, String road)
	{
		public Snapped(double lat, double lon, double distance)
		{
			this(lat, lon, distance, null);
		}
	}

	/** the service can't match the drive at all, e.g. no road near it */
	public static class NoRoadException extends Exception
	{
		NoRoadException(String message)
		{
			super(message);
		}
	}

	/**
	 * @param points
	 *            of one drive, at least two, in the order of time
	 * @return for each point its position on the road, <code>null</code> for a
	 *         point the service left out (e.g. an outlier of the GPS)
	 * @throws NoRoadException
	 *             the drive can't be matched, e.g. off-road
	 * @throws IOException
	 *             the service isn't reachable or failed, to try again later
	 */
	public List<Snapped> match(List<Pending> points) throws NoRoadException, IOException
	{
		List<Map<String, Object>> shape = new ArrayList<>(points.size());
		for (Pending point : points)
		{
			Map<String, Object> location = new LinkedHashMap<>();
			location.put("lat", point.lat());
			location.put("lon", point.lon());
			location.put("time", point.ts().toEpochSecond());
			shape.add(location);
		}
		Map<String, Object> body = Map.of(
				"shape", shape,
				"costing", "auto",
				"shape_match", "map_snap",
				"filters", Map.of("attributes",
						List.of("matched.point", "matched.type", "matched.distance_from_trace_point",
								"matched.edge_index", "edge.begin_shape_index", "edge.end_shape_index", "shape"),
						"action", "include"));

		HttpRequest request = HttpRequest.newBuilder(URI.create(config.url().orElseThrow() + "/trace_attributes"))
				.timeout(config.timeout())
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(body)))
				.build();
		HttpResponse<byte[]> response;
		try
		{
			response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted", e);
		}
		if (response.statusCode() == 400)
		{
			// e.g. 444: no path along the roads, 171: no road near a point
			throw new NoRoadException(error(response));
		}
		if (response.statusCode() != 200)
		{
			// e.g. 404 of a wrong URL: the points wait instead of losing their roads
			throw new IOException("Valhalla answered " + response.statusCode() + ": " + error(response));
		}

		JsonNode answer = mapper.readTree(response.body());
		JsonNode matched = answer.path("matched_points");
		if (matched.size() != points.size())
		{
			throw new IOException("Valhalla answered " + matched.size() + " points for " + points.size());
		}
		double[] road;
		try
		{
			road = Polyline.decode(answer.path("shape").asText(null));
		}
		catch (IllegalArgumentException e)
		{
			// the points stay on the road, only without the road between them
			road = new double[0];
		}
		JsonNode edges = answer.path("edges");

		List<Snapped> snapped = new ArrayList<>(points.size());
		int previous = -1;
		for (int i = 0; i < points.size(); i++)
		{
			JsonNode point = matched.get(i);
			// interpolated: between matched points on the same road, also on it
			String type = point.path("type").asText();
			if (!type.equals("matched") && !type.equals("interpolated"))
			{
				snapped.add(null);
				previous = -1;
				continue;
			}
			double lat = point.path("lat").asDouble();
			double lon = point.path("lon").asDouble();
			int segment = segment(road, edges.path(point.path("edge_index").asInt(-1)), lat, lon);
			String between = null;
			Snapped before = i > 0 ? snapped.get(i - 1) : null;
			if (before != null && previous >= 0 && segment > previous)
			{
				double[] vertices = trim(Arrays.copyOfRange(road, (previous + 1) * 2, (segment + 1) * 2), before,
						lat, lon);
				if (plausible(before, vertices, lat, lon, points.get(i - 1), points.get(i)))
				{
					between = Polyline.encode(vertices);
				}
			}
			snapped.add(new Snapped(lat, lon, point.path("distance_from_trace_point").asDouble(), between));
			previous = segment;
		}
		return snapped;
	}

	/**
	 * @return the index of the piece of the shape the point lies on, between its
	 *         vertex and the next one, of the edge of the point; -1 without
	 */
	static int segment(double[] shape, JsonNode edge, double lat, double lon)
	{
		int vertices = shape.length / 2;
		int begin = edge.path("begin_shape_index").asInt(-1);
		int end = edge.path("end_shape_index").asInt(-1);
		if (begin < 0 || end < begin || end >= vertices)
		{
			return -1;
		}
		int nearest = begin;
		double best = Double.MAX_VALUE;
		for (int k = begin; k < Math.max(begin + 1, end); k++)
		{
			double distance = k + 1 < vertices
					? distanceToSegment(lat, lon, shape[k * 2], shape[k * 2 + 1], shape[k * 2 + 2], shape[k * 2 + 3])
					: distance(lat, lon, shape[k * 2], shape[k * 2 + 1]);
			if (distance < best)
			{
				best = distance;
				nearest = k;
			}
		}
		return nearest;
	}

	/**
	 * @return without a first or last vertex on the point itself: a point is often
	 *         at the end of a piece, e.g. on a junction
	 */
	private static double[] trim(double[] vertices, Snapped before, double lat, double lon)
	{
		int from = 0;
		int to = vertices.length;
		if (to > from && distance(before.lat(), before.lon(), vertices[from], vertices[from + 1]) < SAME_METERS)
		{
			from += 2;
		}
		if (to > from && distance(lat, lon, vertices[to - 2], vertices[to - 1]) < SAME_METERS)
		{
			to -= 2;
		}
		return from == 0 && to == vertices.length ? vertices : Arrays.copyOfRange(vertices, from, to);
	}

	/** whether the vertices are the road between the points, not a jump */
	private static boolean plausible(Snapped before, double[] vertices, double lat, double lon, Pending from,
			Pending to)
	{
		double length = 0;
		double previousLat = before.lat();
		double previousLon = before.lon();
		for (int k = 0; k < vertices.length; k += 2)
		{
			length += distance(previousLat, previousLon, vertices[k], vertices[k + 1]);
			previousLat = vertices[k];
			previousLon = vertices[k + 1];
		}
		length += distance(previousLat, previousLon, lat, lon);
		double straight = distance(before.lat(), before.lon(), lat, lon);
		double seconds = Math.max(1, Duration.between(from.ts(), to.ts()).toMillis() / 1000.0);
		return length <= straight * DETOUR_FACTOR + DETOUR_METERS && length / seconds <= MAX_ROAD_SPEED;
	}

	private static double distance(double lat1, double lon1, double lat2, double lon2)
	{
		return TrackSimplifier.distance(new TrackPoint(null, lat1, lon1, null), new TrackPoint(null, lat2, lon2, null));
	}

	private static double distanceToSegment(double lat, double lon, double lat1, double lon1, double lat2,
			double lon2)
	{
		return TrackSimplifier.distanceToSegment(new TrackPoint(null, lat, lon, null),
				new TrackPoint(null, lat1, lon1, null), new TrackPoint(null, lat2, lon2, null));
	}

	/** the message of Valhalla, else the status */
	private String error(HttpResponse<byte[]> response)
	{
		try
		{
			return mapper.readTree(response.body()).path("error").asText("status " + response.statusCode());
		}
		catch (IOException e)
		{
			return "status " + response.statusCode();
		}
	}
}
