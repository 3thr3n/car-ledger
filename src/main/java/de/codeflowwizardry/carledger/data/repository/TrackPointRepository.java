package de.codeflowwizardry.carledger.data.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;

import javax.sql.DataSource;

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
}
