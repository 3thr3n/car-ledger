package de.codeflowwizardry.carledger.csv;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import de.codeflowwizardry.carledger.data.UnitSystem;

/**
 * Converts the values of an imported row from the units of the file to the
 * ones stored: km, liters, cent per liter and l/100km. A missing value (zero)
 * stays zero.
 */
public final class ImportUnits
{
	/** exact by definition */
	static final BigDecimal KM_PER_MILE = new BigDecimal("1.609344");
	static final BigDecimal LITERS_PER_US_GALLON = new BigDecimal("3.785411784");
	static final BigDecimal LITERS_PER_IMPERIAL_GALLON = new BigDecimal("4.54609");
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
	private static final MathContext PRECISION = MathContext.DECIMAL64;

	private final UnitSystem system;

	private ImportUnits(UnitSystem system)
	{
		this.system = system;
	}

	/**
	 * @param system
	 *            units of the file, metric without one
	 */
	public static ImportUnits of(UnitSystem system)
	{
		return new ImportUnits(system == null ? UnitSystem.METRIC : system);
	}

	private boolean miles()
	{
		return system != UnitSystem.METRIC;
	}

	private boolean gallons()
	{
		return system == UnitSystem.US;
	}

	/**
	 * @return in km
	 */
	public BigDecimal distance(BigDecimal value)
	{
		return miles() && value != null ? scale(value.multiply(KM_PER_MILE)) : value;
	}

	/**
	 * @return in whole km
	 */
	public Integer odometer(Integer value)
	{
		if (!miles() || value == null)
		{
			return value;
		}
		return new BigDecimal(value).multiply(KM_PER_MILE).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	/**
	 * @return in liters
	 */
	public BigDecimal volume(BigDecimal value)
	{
		return gallons() && value != null ? scale(value.multiply(LITERS_PER_US_GALLON)) : value;
	}

	/**
	 * @param value
	 *            cent per liter, in the US the currency per gallon as at the pump
	 * @return in cent per liter
	 */
	public BigDecimal fuelPrice(BigDecimal value)
	{
		return gallons() && value != null
				? scale(value.multiply(HUNDRED).divide(LITERS_PER_US_GALLON, PRECISION))
				: value;
	}

	/**
	 * @param value
	 *            l/100km, mpg with miles (US or imperial gallons)
	 * @return in l/100km
	 */
	public BigDecimal consumption(BigDecimal value)
	{
		if (!miles() || value == null || value.signum() == 0)
		{
			return value;
		}
		BigDecimal litersPerGallon = gallons() ? LITERS_PER_US_GALLON : LITERS_PER_IMPERIAL_GALLON;
		// mpg and l/100km are inverse: 100 * liters per gallon / km per mile / mpg
		return scale(HUNDRED.multiply(litersPerGallon).divide(KM_PER_MILE.multiply(value), PRECISION));
	}

	private static BigDecimal scale(BigDecimal value)
	{
		return value.setScale(2, RoundingMode.HALF_UP);
	}
}
