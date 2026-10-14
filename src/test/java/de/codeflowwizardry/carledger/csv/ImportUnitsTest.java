package de.codeflowwizardry.carledger.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.UnitSystem;

class ImportUnitsTest
{
	@Test
	void shouldKeepMetricValues()
	{
		ImportUnits units = ImportUnits.of(null);
		assertEquals(new BigDecimal("512.3"), units.distance(new BigDecimal("512.3")));
		assertEquals(new BigDecimal("41.02"), units.volume(new BigDecimal("41.02")));
		assertEquals(new BigDecimal("172.9"), units.fuelPrice(new BigDecimal("172.9")));
		assertEquals(new BigDecimal("6.4"), units.consumption(new BigDecimal("6.4")));
		assertEquals(42_000, units.odometer(42_000));
	}

	@Test
	void shouldConvertMilesGallonsAndMpgOfTheUs()
	{
		ImportUnits units = ImportUnits.of(UnitSystem.US);
		assertEquals(new BigDecimal("482.80"), units.distance(new BigDecimal("300")));
		assertEquals(new BigDecimal("37.85"), units.volume(BigDecimal.TEN));
		// $3.459 per gallon
		assertEquals(new BigDecimal("91.38"), units.fuelPrice(new BigDecimal("3.459")));
		// 30 mpg (US)
		assertEquals(new BigDecimal("7.84"), units.consumption(new BigDecimal("30")));
		assertEquals(80_467, units.odometer(50_000));
	}

	@Test
	void shouldKeepLitersInTheUk()
	{
		ImportUnits units = ImportUnits.of(UnitSystem.UK);
		assertEquals(new BigDecimal("482.80"), units.distance(new BigDecimal("300")));
		assertEquals(BigDecimal.TEN, units.volume(BigDecimal.TEN));
		// pence per liter as at the pump
		assertEquals(new BigDecimal("145.9"), units.fuelPrice(new BigDecimal("145.9")));
		// 47.1 mpg (imperial)
		assertEquals(new BigDecimal("6.00"), units.consumption(new BigDecimal("47.08")));
	}

	@Test
	void shouldKeepMissingValues()
	{
		ImportUnits units = ImportUnits.of(UnitSystem.US);
		assertEquals(BigDecimal.ZERO, units.consumption(BigDecimal.ZERO));
		assertNull(units.distance(null));
		assertNull(units.odometer(null));
		assertEquals(new BigDecimal("0.00"), units.volume(BigDecimal.ZERO));
	}
}
