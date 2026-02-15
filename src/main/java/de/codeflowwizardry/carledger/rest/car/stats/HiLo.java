package de.codeflowwizardry.carledger.rest.car.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record HiLo(BigDecimal min, BigDecimal max, int scale)
{
	@Override
	public BigDecimal min()
	{
		return min.setScale(scale, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal max()
	{
		return max.setScale(scale, RoundingMode.HALF_UP);
	}

	@Override
	@JsonIgnore
	@SuppressWarnings("java:S6207")
	public int scale()
	{
		return scale;
	}
}
