package de.codeflowwizardry.carledger.rest.records;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

class BillPojoTest
{
	@Test
	void shouldReturnZero()
	{
		assertEquals(BigDecimal.ZERO, BillPojo.calculateConsumption(BigDecimal.valueOf(0), BigDecimal.valueOf(250.0)));
		assertEquals(BigDecimal.ZERO, BillPojo.calculateConsumption(BigDecimal.valueOf(8), BigDecimal.valueOf(0.0)));
	}

	@Test
	void shouldCalculate()
	{
		assertEquals(BigDecimal.valueOf(4.00).setScale(4, RoundingMode.HALF_UP),
				BillPojo.calculateConsumption(BigDecimal.valueOf(8), BigDecimal.valueOf(200)));
	}

	@Test
	void shouldCalculatePrice()
	{
		assertEquals(BigDecimal.valueOf(20.00).setScale(2, RoundingMode.HALF_UP),
				BillPojo.calculatePrice(BigDecimal.valueOf(10), BigDecimal.valueOf(200)));
	}
}
