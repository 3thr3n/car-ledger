package de.codeflowwizardry.carledger.data;

import static java.math.BigDecimal.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.builders.FuelBillEntityBuilder;
import de.codeflowwizardry.carledger.rest.records.FuelBillInput;

public class FuelBillEntityTest
{
	@Test
	void shouldCalculateEverything()
	{
		// given
		BigDecimal distance = valueOf(410.2);
		BigDecimal estimate = valueOf(7.8);
		BigDecimal pricePerUnit = valueOf(174.9);
		BigDecimal avgConsumption = valueOf(7.45);

		BigInteger vatRate = BigInteger.valueOf(19);

		BigDecimal netTotal = valueOf(44.94);
		BigDecimal ustTotal = valueOf(8.54);
		BigDecimal total = valueOf(53.48);

		FuelBillInput input = new FuelBillInput(LocalDate.of(2025, 12, 9), distance, valueOf(30.58), pricePerUnit,
				estimate, vatRate, total);

		// when
		FuelBillEntity bill = FuelBillEntityBuilder.toEntity(input);

		// then
		assertThat(bill.getDistance(), is(distance));
		assertThat(bill.getPricePerUnit(), is(pricePerUnit));
		assertThat(bill.getVatRate(), is(vatRate));
		assertThat(bill.getEstimate(), is(estimate));
//		assertThat(bill.getAvgConsumption(), is(avgConsumption));

		assertThat(bill.getNetAmount(), is(netTotal));
		assertThat(bill.getUstAmount(), is(ustTotal));
		assertThat(bill.getTotal(), is(total));
	}

	@Test
	void shouldAllowMinimalConfig()
	{
		// given
		LocalDate date = LocalDate.of(2025, 12, 9);
		BigDecimal unit = valueOf(30.58);
		BigDecimal pricePerUnit = valueOf(174.9);

		BigInteger vatRate = BigInteger.valueOf(19);

		BigDecimal total = valueOf(53.48);

		FuelBillInput input = new FuelBillInput(date, unit, pricePerUnit, vatRate);

		// when
		FuelBillEntity bill = FuelBillEntityBuilder.toEntity(input);

		// then
		assertThat(bill.getPricePerUnit(), is(pricePerUnit));
		assertThat(bill.getVatRate(), is(vatRate));

		assertThat(bill.getNetAmount(), is(valueOf(44.94)));
		assertThat(bill.getUstAmount(), is(valueOf(8.54)));
		assertThat(bill.getTotal(), is(total));
	}
}
