package de.codeflowwizardry.carledger.builders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.rest.records.FuelBillInput;

class FuelBillEntityBuilderTest
{
	@Test
	void shouldFailBecauseNothingSet()
	{
		assertThrows(IllegalArgumentException.class, () -> new FuelBillEntityBuilder().build(),
				"At least two of 'unit', 'total' or 'pricePerUnit' must be set!");
	}

	@Test
	void shouldFailWithoutVatRate()
	{
		assertThrows(IllegalArgumentException.class, () -> new FuelBillEntityBuilder()
				.setUnit(BigDecimal.valueOf(20))
				.setTotal(BigDecimal.TEN)
				.build(),
				"Vat rate cannot be null!");
	}

	@Test
	void shouldTryCalculatingUnit()
	{
		// given
		FuelBillEntityBuilder fuelBillEntityBuilder = new FuelBillEntityBuilder()
				.setTotal(BigDecimal.valueOf(100))
				.setVatRate(BigInteger.ZERO)
				.setPricePerUnit(BigDecimal.valueOf(10));

		// when
		FuelBillEntity result = fuelBillEntityBuilder.build();

		// then
		BigDecimal expected = BigDecimal.valueOf(10).setScale(2, RoundingMode.HALF_UP);
		assertEquals(expected, result.getUnit());
	}

	@Test
	void shouldTryCalculatingUnitWithVat()
	{
		// given
		FuelBillEntityBuilder fuelBillEntityBuilder = new FuelBillEntityBuilder()
				.setTotal(BigDecimal.valueOf(100))
				.setVatRate(BigInteger.valueOf(25))
				.setPricePerUnit(BigDecimal.valueOf(10));

		// when
		FuelBillEntity result = fuelBillEntityBuilder.build();

		// then
		BigDecimal expected = BigDecimal.valueOf(8).setScale(2, RoundingMode.HALF_UP);
		assertEquals(expected, result.getUnit());
	}

	@Test
	void shouldCalculateNetAndUstAmount()
	{
		// given
		FuelBillEntityBuilder fuelBillEntityBuilder = new FuelBillEntityBuilder()
				.setTotal(BigDecimal.valueOf(100))
				.setVatRate(BigInteger.valueOf(25))
				.setPricePerUnit(BigDecimal.valueOf(10));

		// when
		FuelBillEntity result = fuelBillEntityBuilder.build();

		// then
		BigDecimal expectedUst = BigDecimal.valueOf(20).setScale(2, RoundingMode.HALF_UP);
		BigDecimal expectedNet = BigDecimal.valueOf(80).setScale(2, RoundingMode.HALF_UP);

		assertEquals(expectedUst, result.getUstAmount());
		assertEquals(expectedNet, result.getNetAmount());
	}

	@Test
	void shouldTryCalculatingTotal()
	{
		// given
		FuelBillEntityBuilder fuelBillEntityBuilder = new FuelBillEntityBuilder()
				.setUnit(BigDecimal.valueOf(8))
				.setVatRate(BigInteger.valueOf(25))
				.setPricePerUnit(BigDecimal.valueOf(10));

		// when
		FuelBillEntity result = fuelBillEntityBuilder.build();

		// then
		BigDecimal expected = BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
		assertEquals(expected, result.getTotal());
	}

	@Test
	void shouldConvertToEntity()
	{
		// given
		LocalDate date = LocalDate.of(2024, 5, 10);
		FuelBillInput fuelBillInput = new FuelBillInput(date, BigDecimal.valueOf(20),
				BigDecimal.valueOf(1), BigInteger.ZERO);

		// when
		FuelBillEntity result = FuelBillEntityBuilder.toEntity(fuelBillInput);

		// then
		assertEquals(BigDecimal.valueOf(20).setScale(2, RoundingMode.HALF_UP), result.getTotal());
		assertEquals(BigDecimal.valueOf(1), result.getPricePerUnit());
		assertEquals(date, result.getDate());
	}
}
