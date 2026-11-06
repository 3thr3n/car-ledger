package de.codeflowwizardry.carledger.data.repository;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyIn;

import de.codeflowwizardry.carledger.rest.car.track.GForces;
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;
import de.codeflowwizardry.carledger.rest.device.TrackPointInput;
import de.codeflowwizardry.carledger.tracker.MapMatchingConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;

/**
 * The points of the GPS trackers. They come in batches of hundreds, so they are
 * written with JDBC and not as entities.
 */
@ApplicationScoped
public class TrackPointRepository
{
	private static final String INSERT = """
			INSERT INTO track_point (tk_device_id, tk_car_id, tk_ts, tk_lat, tk_lon, tk_speed, tk_alt, tk_hdop,
				tk_accel, tk_brake, tk_lateral, tk_gsource, tk_match)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			ON CONFLICT (tk_device_id, tk_ts) DO NOTHING""";

	private static final String SELECT = """
			SELECT tk_ts, tk_lat, tk_lon, tk_speed, tk_accel, tk_brake, tk_lateral, tk_gsource FROM track_point
			WHERE tk_car_id = ? AND tk_ts >= ? AND tk_ts < ?
			ORDER BY tk_ts, id""";

	/** the same, a matched point at its position on the road */
	private static final String SELECT_MATCHED = SELECT.replace("SELECT tk_ts, tk_lat, tk_lon,",
			"SELECT tk_ts, CASE WHEN tk_match = 'M' THEN tk_match_lat ELSE tk_lat END,"
					+ " CASE WHEN tk_match = 'M' THEN tk_match_lon ELSE tk_lon END,");

	/** waiting for the matching: P, see V1.37.0__map_matching.sql */
	public static final String PENDING = "P";
	public static final String MATCHED = "M";
	public static final String UNMATCHED = "U";

	/** rows per round trip, a month of points isn't loaded at once */
	private static final int FETCH_SIZE = 2000;

	private final DataSource dataSource;
	/** new points wait for the matching only with a service */
	private final boolean matching;

	@Inject
	public TrackPointRepository(DataSource dataSource, MapMatchingConfig matchingConfig)
	{
		this.dataSource = dataSource;
		this.matching = matchingConfig.url().isPresent();
	}

	/**
	 * Inserts the points in the transaction of the caller, a point of the device
	 * with the same time is skipped.
	 *
	 * @return the amount of points inserted
	 */
	public int insert(long deviceId, long carId, List<TrackPointInput> points)
	{
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(INSERT))
		{
			for (TrackPointInput point : points)
			{
				statement.setLong(1, deviceId);
				statement.setLong(2, carId);
				statement.setObject(3, point.ts());
				statement.setDouble(4, point.lat());
				statement.setDouble(5, point.lon());
				statement.setObject(6, point.speed(), Types.DOUBLE);
				statement.setObject(7, point.alt(), Types.DOUBLE);
				statement.setObject(8, point.hdop(), Types.DOUBLE);
				statement.setObject(9, point.accel(), Types.DOUBLE);
				statement.setObject(10, point.brake(), Types.DOUBLE);
				statement.setObject(11, point.lateral(), Types.DOUBLE);
				statement.setString(12, point.gsource());
				statement.setString(13, matching ? PENDING : null);
				statement.addBatch();
			}

			int inserted = 0;
			for (int count : statement.executeBatch())
			{
				inserted += count;
			}
			return inserted;
		}
		catch (SQLException e)
		{
			throw new PersistenceException("Track points of device " + deviceId + " could not be stored", e);
		}
	}

	/**
	 * Passes the points of the car in the range to the consumer, in the order of
	 * time. Needs a transaction: the rows are read with a cursor.
	 *
	 * @param to
	 *            exclusive
	 */
	public void forEach(long carId, OffsetDateTime from, OffsetDateTime to, Consumer<TrackPoint> consumer)
	{
		forEach(carId, from, to, false, consumer);
	}

	/**
	 * @param to
	 *            exclusive
	 * @param matched
	 *            the matched points at their position on the road, e.g. for the
	 *            map; the others at the position of the GPS
	 */
	public void forEach(long carId, OffsetDateTime from, OffsetDateTime to, boolean matched,
			Consumer<TrackPoint> consumer)
	{
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(matched ? SELECT_MATCHED : SELECT))
		{
			statement.setFetchSize(FETCH_SIZE);
			statement.setLong(1, carId);
			statement.setObject(2, from);
			statement.setObject(3, to);
			try (ResultSet result = statement.executeQuery())
			{
				while (result.next())
				{
					consumer.accept(new TrackPoint(result.getObject(1, OffsetDateTime.class), result.getDouble(2),
							result.getDouble(3), result.getObject(4, Double.class),
							GForces.of(result.getObject(5, Double.class), result.getObject(6, Double.class),
									result.getObject(7, Double.class), result.getString(8))));
				}
			}
		}
		catch (SQLException e)
		{
			throw new PersistenceException("Track points of car " + carId + " could not be read", e);
		}
	}

	/** a point waiting for the matching */
	public record Pending(long id, OffsetDateTime ts, double lat, double lon)
	{
	}

	/**
	 * @param state
	 *            {@link #MATCHED} with the position on the road, else
	 *            {@link #UNMATCHED}
	 */
	public record Match(long id, String state, Double lat, Double lon)
	{
	}

	/**
	 * @return the cars with points waiting for the matching that were sent before
	 *         the time
	 */
	public List<Long> carsWithPending(OffsetDateTime before)
	{
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT DISTINCT tk_car_id FROM track_point WHERE tk_match = 'P' AND tk_ts < ?"))
		{
			statement.setObject(1, before);
			List<Long> cars = new ArrayList<>();
			try (ResultSet result = statement.executeQuery())
			{
				while (result.next())
				{
					cars.add(result.getLong(1));
				}
			}
			return cars;
		}
		catch (SQLException e)
		{
			throw new PersistenceException("Cars with points to match could not be read", e);
		}
	}

	/**
	 * @return the oldest points of the car waiting for the matching, before the
	 *         time, in the order of time
	 */
	public List<Pending> pending(long carId, OffsetDateTime before, int limit)
	{
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT id, tk_ts, tk_lat, tk_lon"
						+ " FROM track_point WHERE tk_car_id = ? AND tk_match = 'P' AND tk_ts < ?"
						+ " ORDER BY tk_ts, id LIMIT ?"))
		{
			statement.setLong(1, carId);
			statement.setObject(2, before);
			statement.setInt(3, limit);
			List<Pending> points = new ArrayList<>();
			try (ResultSet result = statement.executeQuery())
			{
				while (result.next())
				{
					points.add(new Pending(result.getLong(1), result.getObject(2, OffsetDateTime.class),
							result.getDouble(3), result.getDouble(4)));
				}
			}
			return points;
		}
		catch (SQLException e)
		{
			throw new PersistenceException("Track points of car " + carId + " to match could not be read", e);
		}
	}

	/**
	 * @return whether the car has a point after the time, up to the other one
	 *         (inclusive): a drive on the road then isn't finished
	 */
	public boolean hasPoint(long carId, OffsetDateTime after, OffsetDateTime until)
	{
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT 1 FROM track_point WHERE tk_car_id = ? AND tk_ts > ? AND tk_ts <= ? LIMIT 1"))
		{
			statement.setLong(1, carId);
			statement.setObject(2, after);
			statement.setObject(3, until);
			try (ResultSet result = statement.executeQuery())
			{
				return result.next();
			}
		}
		catch (SQLException e)
		{
			throw new PersistenceException("Track points of car " + carId + " could not be read", e);
		}
	}

	/**
	 * Stores the results of the matching, of points still waiting for it: a
	 * point deleted or matched by another instance meanwhile is skipped
	 */
	public void saveMatches(List<Match> matches)
	{
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("UPDATE track_point"
						+ " SET tk_match = ?, tk_match_lat = ?, tk_match_lon = ? WHERE id = ? AND tk_match = 'P'"))
		{
			for (Match match : matches)
			{
				statement.setString(1, match.state());
				statement.setObject(2, match.lat(), Types.DOUBLE);
				statement.setObject(3, match.lon(), Types.DOUBLE);
				statement.setLong(4, match.id());
				statement.addBatch();
			}
			statement.executeBatch();
		}
		catch (SQLException e)
		{
			throw new PersistenceException("Matches of track points could not be stored", e);
		}
	}

	/**
	 * @return whether the car has a point
	 */
	public boolean exists(long carId)
	{
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT 1 FROM track_point WHERE tk_car_id = ? LIMIT 1"))
		{
			statement.setLong(1, carId);
			try (ResultSet result = statement.executeQuery())
			{
				return result.next();
			}
		}
		catch (SQLException e)
		{
			throw new PersistenceException("Track points of car " + carId + " could not be read", e);
		}
	}

	/**
	 * Writes the points of the car as CSV, the oldest first: the index of the
	 * tracker, the time in epoch seconds, latitude and longitude with 6 decimals
	 * (about 0.1 m), speed, altitude and hdop with 1, an empty field for a missing
	 * value; then accel, brake and lateral with 2 decimals and the source of
	 * them. Straight from the database, e.g. for the backup.
	 *
	 * @param trackers
	 *            the index of each tracker of the car, by its id
	 * @return the amount of points written
	 */
	public long copyOut(long carId, Map<Long, Integer> trackers, OutputStream out)
	{
		if (trackers.isEmpty())
		{
			return 0;
		}
		StringBuilder index = new StringBuilder("CASE tk_device_id");
		// ids and indexes are numbers, nothing of a user ends up in the statement
		trackers.forEach((id, i) -> index.append(" WHEN ").append((long) id).append(" THEN ").append((int) i));
		index.append(" END");
		String sql = "COPY (SELECT " + index + ", trim_scale(extract(epoch FROM tk_ts)),"
				+ " trim_scale(round(tk_lat::numeric, 6)), trim_scale(round(tk_lon::numeric, 6)),"
				+ " trim_scale(round(tk_speed::numeric, 1)), trim_scale(round(tk_alt::numeric, 1)),"
				+ " trim_scale(round(tk_hdop::numeric, 1)), trim_scale(round(tk_accel::numeric, 2)),"
				+ " trim_scale(round(tk_brake::numeric, 2)), trim_scale(round(tk_lateral::numeric, 2)), tk_gsource"
				+ " FROM track_point WHERE tk_car_id = " + carId + " ORDER BY tk_ts, id) TO STDOUT WITH (FORMAT csv)";
		try (Connection connection = dataSource.getConnection())
		{
			return connection.unwrap(PGConnection.class).getCopyAPI().copyOut(sql, out);
		}
		catch (SQLException | IOException e)
		{
			throw new PersistenceException("Track points of car " + carId + " could not be written", e);
		}
	}

	/**
	 * Inserts many points at once with <code>COPY</code>, e.g. of a restore, in
	 * the transaction of the caller: {@link Bulk#add} the points, then
	 * {@link Bulk#finish}. A point of a tracker with the same time is skipped.
	 */
	public Bulk bulk(long carId)
	{
		return new Bulk(carId);
	}

	public final class Bulk implements AutoCloseable
	{
		private final long carId;
		private final Connection connection;
		private final CopyIn copy;
		private final StringBuilder rows = new StringBuilder();

		private Bulk(long carId)
		{
			this.carId = carId;
			try
			{
				connection = dataSource.getConnection();
				try (Statement statement = connection.createStatement())
				{
					// one per connection, emptied for each car
					statement.execute("CREATE TEMP TABLE IF NOT EXISTS bulk_track_point (tk_device_id BIGINT,"
							+ " tk_ts TIMESTAMP WITH TIME ZONE, tk_lat DOUBLE PRECISION, tk_lon DOUBLE PRECISION,"
							+ " tk_speed DOUBLE PRECISION, tk_alt DOUBLE PRECISION, tk_hdop DOUBLE PRECISION,"
							+ " tk_accel DOUBLE PRECISION, tk_brake DOUBLE PRECISION, tk_lateral DOUBLE PRECISION,"
							+ " tk_gsource VARCHAR(10)) ON COMMIT DROP");
					statement.execute("TRUNCATE bulk_track_point");
				}
				copy = connection.unwrap(PGConnection.class).getCopyAPI()
						.copyIn("COPY bulk_track_point FROM STDIN WITH (FORMAT csv)");
			}
			catch (SQLException e)
			{
				throw new PersistenceException("Track points of car " + carId + " could not be stored", e);
			}
		}

		public void add(long deviceId, TrackPointInput point)
		{
			rows.append(deviceId).append(',').append(point.ts()).append(',').append(point.lat()).append(',')
					.append(point.lon()).append(',').append(value(point.speed())).append(',')
					.append(value(point.alt())).append(',').append(value(point.hdop())).append(',')
					.append(value(point.accel())).append(',').append(value(point.brake())).append(',')
					.append(value(point.lateral())).append(',')
					// only imu, gps or unknown, nothing to quote
					.append(point.gsource() == null ? "" : point.gsource()).append('\n');
			if (rows.length() > 64 * 1024)
			{
				flush();
			}
		}

		/**
		 * @return the amount of points inserted
		 */
		public int finish()
		{
			flush();
			try
			{
				copy.endCopy();
				try (Statement statement = connection.createStatement())
				{
					// restored points are matched again, the matches aren't part of the backup
					return statement.executeUpdate("INSERT INTO track_point (tk_device_id, tk_car_id, tk_ts, tk_lat,"
							+ " tk_lon, tk_speed, tk_alt, tk_hdop, tk_accel, tk_brake, tk_lateral, tk_gsource, tk_match)"
							+ " SELECT tk_device_id, " + carId + ", tk_ts, tk_lat, tk_lon, tk_speed, tk_alt, tk_hdop,"
							+ " tk_accel, tk_brake, tk_lateral, tk_gsource, "
							+ (matching ? "'" + PENDING + "'" : "NULL")
							+ " FROM bulk_track_point ORDER BY tk_ts ON CONFLICT (tk_device_id, tk_ts) DO NOTHING");
				}
			}
			catch (SQLException e)
			{
				throw new PersistenceException("Track points of car " + carId + " could not be stored", e);
			}
		}

		private void flush()
		{
			if (rows.isEmpty())
			{
				return;
			}
			byte[] bytes = rows.toString().getBytes(StandardCharsets.UTF_8);
			rows.setLength(0);
			try
			{
				copy.writeToCopy(bytes, 0, bytes.length);
			}
			catch (SQLException e)
			{
				throw new PersistenceException("Track points of car " + carId + " could not be stored", e);
			}
		}

		@Override
		public void close()
		{
			try
			{
				if (copy.isActive())
				{
					copy.cancelCopy();
				}
				connection.close();
			}
			catch (SQLException e)
			{
				// the transaction rolls back anyway
			}
		}

		private static String value(Double value)
		{
			return value == null ? "" : value.toString();
		}
	}

	/**
	 * Deletes the points of the car in the range, open on a side without a time
	 *
	 * @param from
	 *            inclusive
	 * @param to
	 *            exclusive, the points of this time stay
	 * @return the amount of points deleted
	 */
	public int delete(long carId, OffsetDateTime from, OffsetDateTime to)
	{
		String sql = "DELETE FROM track_point WHERE tk_car_id = ?" + (from == null ? "" : " AND tk_ts >= ?")
				+ (to == null ? "" : " AND tk_ts < ?");
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql))
		{
			int index = 1;
			statement.setLong(index++, carId);
			if (from != null)
			{
				statement.setObject(index++, from);
			}
			if (to != null)
			{
				statement.setObject(index, to);
			}
			return statement.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new PersistenceException("Track points of car " + carId + " could not be deleted", e);
		}
	}
}
