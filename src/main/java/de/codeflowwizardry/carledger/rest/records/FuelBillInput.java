package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;

import static de.codeflowwizardry.carledger.StatsCalculator.ONE_HUNDRED;

public record FuelBillInput(LocalDate day, BigDecimal distance, BigDecimal unit, BigDecimal pricePerUnit,
		BigDecimal estimate, BigInteger vatRate, BigDecimal total)
{
	public FuelBillInput(LocalDate day, BigDecimal unit, BigDecimal pricePerUnit, BigInteger vatRate) {
        this(day, null, unit, pricePerUnit, null, vatRate, null);
    }

	public BigDecimal getCalculativeVat() {
		return null;
	}

	public BigDecimal calculateAverageConsumption() {
		if (distance == null || unit == null) {
			return null;
		}

		return BigDecimal.ZERO.compareTo(distance) == 0
				? null
				: unit.multiply(ONE_HUNDRED)
				.divide(distance, RoundingMode.HALF_UP);
	}
}
