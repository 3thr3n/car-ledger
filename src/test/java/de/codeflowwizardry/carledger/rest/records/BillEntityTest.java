package de.codeflowwizardry.carledger.rest.records;

import static de.codeflowwizardry.carledger.rest.records.Bill.calculateConsumption;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.builders.FuelBillEntityBuilder;

class BillEntityTest
{
	@Test
	void shouldReturnZero()
	{
		assertEquals(BigDecimal.ZERO, calculateConsumption(BigDecimal.valueOf(0), BigDecimal.valueOf(250.0)));
		assertEquals(BigDecimal.ZERO, calculateConsumption(BigDecimal.valueOf(8), BigDecimal.valueOf(0.0)));
	}

	@Test
	void shouldCalculate()
	{
		assertEquals(BigDecimal.valueOf(4.00).setScale(4, RoundingMode.HALF_UP),
				calculateConsumption(BigDecimal.valueOf(8), BigDecimal.valueOf(200)));
	}

	@Test
	void shouldCalculatePrice()
	{
		FuelBill bill = FuelBill.convert(
				FuelBillEntityBuilder.toEntity(
						new FuelBillInput(LocalDate.now(), BigDecimal.ZERO, BigDecimal.valueOf(10),
								BigDecimal.valueOf(200), BigDecimal.ZERO, BigInteger.ZERO, BigDecimal.ZERO)));

		assertEquals("20.00", bill.getCalculatedPrice());
	}
}
