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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyIn;

import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;
import de.codeflowwizardry.carledger.rest.device.TrackPointInput;
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
			INSERT INTO track_point (tk_device_id, tk_car_id, tk_ts, tk_lat, tk_lon, tk_speed, tk_alt, tk_hdop)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?)
			ON CONFLICT (tk_device_id, tk_ts) DO NOTHING""";

	private static final String SELECT = """
			SELECT tk_ts, tk_lat, tk_lon, tk_speed FROM track_point
			WHERE tk_car_id = ? AND tk_ts >= ? AND tk_ts < ?
			ORDER BY tk_ts, id""";

	/** rows per round trip, a month of points isn't loaded at once */
	private static final int FETCH_SIZE = 2000;

	private final DataSource dataSource;

	@Inject
	public TrackPointRepository(DataSource dataSource)
	{
		this.dataSource = dataSource;
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
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(SELECT))
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
							result.getDouble(3), result.getObject(4, Double.class)));
				}
			}
		}
		catch (SQLException e)
		{
			throw new PersistenceException("Track points of car " + carId + " could not be read", e);
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
	 * value. Straight from the database, e.g. for the backup.
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
				+ " trim_scale(round(tk_hdop::numeric, 1))"
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
	 * the transaction of the caller: {@link #add} the points, then
	 * {@link #finish}. A point of a tracker with the same time is skipped.
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
							+ " tk_speed DOUBLE PRECISION, tk_alt DOUBLE PRECISION, tk_hdop DOUBLE PRECISION)"
							+ " ON COMMIT DROP");
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
					.append(value(point.alt())).append(',').append(value(point.hdop())).append('\n');
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
					return statement.executeUpdate("INSERT INTO track_point (tk_device_id, tk_car_id, tk_ts, tk_lat,"
							+ " tk_lon, tk_speed, tk_alt, tk_hdop) SELECT tk_device_id, " + carId + ", tk_ts, tk_lat,"
							+ " tk_lon, tk_speed, tk_alt, tk_hdop FROM bulk_track_point ORDER BY tk_ts"
							+ " ON CONFLICT (tk_device_id, tk_ts) DO NOTHING");
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
