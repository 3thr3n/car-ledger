package de.codeflowwizardry.carledger.rest.records;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.Bill;

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
		BillPojo billPojo = BillPojo.convert(new Bill(new BillInputPojo(LocalDate.now(), BigDecimal.ZERO,
				BigDecimal.valueOf(10), BigDecimal.valueOf(200), BigDecimal.ZERO)));

		assertEquals("20.00", billPojo.getCalculatedPrice());
	}
}
