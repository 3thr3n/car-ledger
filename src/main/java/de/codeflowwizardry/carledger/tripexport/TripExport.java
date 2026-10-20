package de.codeflowwizardry.carledger.tripexport;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import de.codeflowwizardry.carledger.data.TripEntity;
import de.codeflowwizardry.carledger.data.TripType;
import de.codeflowwizardry.carledger.data.UnitSystem;

/**
 * The trip log of a car for a period, as the CSV and the PDF write it
 *
 * @param from
 *            first day, <code>null</code> from the first trip on
 * @param to
 *            last day, <code>null</code> until the last trip
 * @param zone
 *            of the times, the one of the browser
 * @param createdAt
 *            when the export was made
 * @param trips
 *            the oldest first, also the deleted ones
 * @param units
 *            of the account, for the PDF
 * @param language
 *            of the PDF, <code>de</code> or <code>en</code>
 */
public record TripExport(String carName, String licensePlate, LocalDate from, LocalDate to, ZoneId zone,
		OffsetDateTime createdAt, List<TripEntity> trips, UnitSystem units, String language)
{
	/**
	 * @param distance
	 *            in km
	 */
	public record Sum(int trips, int distance)
	{
		Sum plus(int km)
		{
			return new Sum(trips + 1, distance + km);
		}
	}

	/**
	 * @return the finished trips per type that weren't deleted, every type
	 */
	public Map<TripType, Sum> sums()
	{
		Map<TripType, Sum> sums = new EnumMap<>(TripType.class);
		for (TripType type : TripType.values())
		{
			sums.put(type, new Sum(0, 0));
		}
		for (TripEntity trip : trips)
		{
			if (counts(trip))
			{
				sums.compute(trip.getType(), (type, sum) -> sum.plus(trip.getDistance()));
			}
		}
		return sums;
	}

	/**
	 * @return finished and not deleted: it is in the sums
	 */
	public static boolean counts(TripEntity trip)
	{
		return !trip.isOpen() && !trip.isDeleted();
	}

	/**
	 * @return the time in the zone of the export, <code>null</code> stays
	 */
	public OffsetDateTime local(OffsetDateTime time)
	{
		return time == null ? null : time.atZoneSameInstant(zone).toOffsetDateTime();
	}
}
