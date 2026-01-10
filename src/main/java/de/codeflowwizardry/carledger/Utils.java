package de.codeflowwizardry.carledger;

import java.math.BigDecimal;

public class Utils
{
	public static boolean atLeastTwoNotNull(Object a, Object b, Object c)
	{
		int count = 0;
		if (a != null)
			count++;
		if (b != null)
			count++;
		if (c != null)
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
}
