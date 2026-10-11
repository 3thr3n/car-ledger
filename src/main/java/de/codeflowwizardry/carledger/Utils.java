package de.codeflowwizardry.carledger;

import java.math.BigDecimal;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

public class Utils
{
	public static boolean atLeastTwoValuesSet(BigDecimal a, BigDecimal b, BigDecimal c)
	{
		int count = 0;
		if (valueWasSet(a))
			count++;
		if (valueWasSet(b))
			count++;
		if (valueWasSet(c))
			count++;
		return count >= 2;
	}

	/**
	 * Check if the BigDecimal value is grater than 0. <b>Null safe</b>
	 *
	 * @param unit
	 *            to check
	 * @return <code>true</code> if value is set.
	 */
	public static boolean valueWasSet(BigDecimal unit)
	{
		return unit != null && unit.signum() > 0;
	}

	/**
	 * @param errors
	 *            validation errors of an input
	 * @throws BadRequestException
	 *             with the errors as body, if there are any
	 */
	public static void requireValid(List<String> errors)
	{
		if (!errors.isEmpty())
		{
			throw new BadRequestException(Response.status(400).entity(errors).build());
		}
	}
}
