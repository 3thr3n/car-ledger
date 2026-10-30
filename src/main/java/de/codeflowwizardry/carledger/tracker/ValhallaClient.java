package de.codeflowwizardry.carledger.tracker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.codeflowwizardry.carledger.data.repository.TrackPointRepository.Pending;
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
	 * A point on the road
	 *
	 * @param distance
	 *            from the position of the GPS, meters
	 */
	public record Snapped(double lat, double lon, double distance)
	{
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
						List.of("matched.point", "matched.type", "matched.distance_from_trace_point"),
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
		if (response.statusCode() / 100 == 4)
		{
			// e.g. 444: no path along the roads, 171: no road near a point
			throw new NoRoadException(mapper.readTree(response.body()).path("error").asText("status "
					+ response.statusCode()));
		}
		if (response.statusCode() != 200)
		{
			throw new IOException("Valhalla answered " + response.statusCode());
		}

		JsonNode matched = mapper.readTree(response.body()).path("matched_points");
		if (matched.size() != points.size())
		{
			throw new IOException("Valhalla answered " + matched.size() + " points for " + points.size());
		}
		List<Snapped> snapped = new ArrayList<>(points.size());
		for (JsonNode point : matched)
		{
			// interpolated: between matched points on the same road, also on it
			String type = point.path("type").asText();
			snapped.add(type.equals("matched") || type.equals("interpolated")
					? new Snapped(point.path("lat").asDouble(), point.path("lon").asDouble(),
							point.path("distance_from_trace_point").asDouble())
					: null);
		}
		return snapped;
	}
}
