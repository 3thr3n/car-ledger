package de.codeflowwizardry.carledger.data.factory;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.records.input.FuelBillInput;

class FuelBillFactoryTest
{
	@Test
	void shouldFailValidating()
	{
		// given
		FuelBillFactory factory = new FuelBillFactory(null, null);

		// when date not set
		FuelBillInput missingDate = new FuelBillInput(null, null, null, null, null, null, null);
		assertThrows(IllegalArgumentException.class, () -> factory.validate(missingDate),
				"Date cannot be null!");

		FuelBillInput missingVat = new FuelBillInput(LocalDate.now(), null, null, null, null, null, null);
		assertThrows(IllegalArgumentException.class, () -> factory.validate(missingVat),
				"Vat rate cannot be null!");

		FuelBillInput missingKeyValues = new FuelBillInput(LocalDate.now(), null, BigInteger.TEN, null, null,
				BigDecimal.valueOf(199.9), null);
		assertThrows(IllegalArgumentException.class, () -> factory.validate(missingKeyValues),
				"At least two of 'unit', 'total' or 'pricePerUnit' must be set!");
	}

	@Test
	void shouldValidate()
	{
		// given
		FuelBillFactory factory = new FuelBillFactory(null, null);

		// when date not set
		FuelBillInput minimalValidation = new FuelBillInput(LocalDate.now(),
				BigDecimal.TEN,
				BigInteger.TEN,
				null,
				null,
				BigDecimal.valueOf(199.9),
				null);
		assertDoesNotThrow(() -> factory.validate(minimalValidation));
	}

	@Test
	void shouldCalculateUnit()
	{
		// given
		FuelBillInput missingUnit = new FuelBillInput(LocalDate.now(),
				BigDecimal.valueOf(68.78),
				BigInteger.valueOf(19),
				null,
				null,
				BigDecimal.valueOf(181.9),
				null);
		// when
		BigDecimal calculatedUnit = FuelBillFactory.calculateUnit(missingUnit, BigDecimal.valueOf(1.19));

		// then
		assertEquals(BigDecimal.valueOf(3781, 2), calculatedUnit);

	}
}
