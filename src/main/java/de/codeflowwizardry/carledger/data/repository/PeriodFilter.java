package de.codeflowwizardry.carledger.data.repository;

import static de.codeflowwizardry.carledger.data.repository.CarRepository.ALL_CARS;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Where clause of the queries over the cars of a user in a period
 */
final class PeriodFilter
{
	private PeriodFilter()
	{
	}

	/**
	 * @param car
	 *            path of the car in the query, e.g. <code>b.car</code>
	 * @param date
	 *            path of the date in the query, e.g. <code>b.date</code>
	 * @param carId
	 *            or {@link CarRepository#ALL_CARS}
	 * @param params
	 *            receives the parameters of the clause
	 * @return <code>" where ..."</code>, to append to the query
	 */
	static String where(String car, String date, long carId, String username, Optional<LocalDate> from,
			Optional<LocalDate> to, Map<String, Object> params)
	{
		String where = " where " + car + ".user.userId = :userId";
		params.put("userId", username);

		if (carId != ALL_CARS)
		{
			where += " and " + car + ".id = :carId";
			params.put("carId", carId);
		}

		if (from.isPresent())
		{
			where += " and " + date + " >= :from";
			params.put("from", from.get());
		}

		if (to.isPresent())
		{
			where += " and " + date + " <= :to";
			params.put("to", to.get());
		}
		return where;
	}
}
