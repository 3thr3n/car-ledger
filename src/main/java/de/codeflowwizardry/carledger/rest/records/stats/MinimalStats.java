package de.codeflowwizardry.carledger.rest.records.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fasterxml.jackson.annotation.JsonGetter;

public record MinimalStats(BigDecimal total, BigDecimal avgFuelConsumption, HiLo minMaxFuelConsumption,
		BigDecimal avgDistance)
{
	@JsonGetter("total")
	public String getTotal()
	{
		return total.setScale(2, RoundingMode.HALF_UP).toString();
	}

	@JsonGetter("avgDistance")
	public String getAvgDistance()
	{
		return avgDistance.setScale(2, RoundingMode.HALF_UP).toString();
	}

	@JsonGetter("avgFuelConsumption")
	public String getAvgFuelConsumption()
	{
		return avgFuelConsumption.setScale(2, RoundingMode.HALF_UP).toString();
	}
}
