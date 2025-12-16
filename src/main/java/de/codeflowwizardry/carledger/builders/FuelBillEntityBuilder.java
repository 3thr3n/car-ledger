package de.codeflowwizardry.carledger.builders;

import static de.codeflowwizardry.carledger.StatsCalculator.ONE_HUNDRED;
import static de.codeflowwizardry.carledger.Utils.atLeastTwoNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;

import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.rest.records.FuelBillInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class FuelBillEntityBuilder
{
	private final static Logger LOG = LoggerFactory.getLogger(FuelBillEntityBuilder.class);

	private LocalDate date = LocalDate.now();
	private BigDecimal total;
	private BigInteger vatRate;
	private BigDecimal netAmount;
	private BigDecimal ustAmount;
	private BigDecimal unit;
	private BigDecimal pricePerUnit;
	private BigDecimal distance = BigDecimal.ZERO;
	private BigDecimal estimate = BigDecimal.ZERO;
	private BigDecimal avgConsumption = BigDecimal.ZERO;
	private BigDecimal costPerKm = BigDecimal.ZERO;

	private BigDecimal vatFactor;

	/**
	 * Sets the day on which the fuel bill was issued, if not set it will be today's
	 * date
	 *
	 * @param date
	 *            on which fuel bill was issued
	 * @return FuelBillEntityBuilder
	 */
	public FuelBillEntityBuilder setDate(LocalDate date)
	{
		if (date != null)
		{
			this.date = date;
		}
		return this;
	}

	public FuelBillEntityBuilder setTotal(BigDecimal total)
	{
		this.total = total;
		return this;
	}

	public FuelBillEntityBuilder setVatRate(BigInteger vatRate)
	{
		this.vatRate = vatRate;

		BigDecimal vatFactor = new BigDecimal(vatRate);
		this.vatFactor = BigDecimal.ONE.add(vatFactor.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));

		return this;
	}

	/**
	 * The net amount of the bill (without vat)
	 *
	 * @param netAmount
	 *            of current fuel bill
	 * @return FuelBillEntityBuilder
	 */
	public FuelBillEntityBuilder setNetAmount(BigDecimal netAmount)
	{
		this.netAmount = netAmount;
		return this;
	}

	/**
	 * The ust amount of the bill (total - net amount)
	 *
	 * @param ustAmount
	 *            of current fuel bill
	 * @return FuelBillEntityBuilder
	 */
	public FuelBillEntityBuilder setUstAmount(BigDecimal ustAmount)
	{
		this.ustAmount = ustAmount;
		return this;
	}

	public FuelBillEntityBuilder setUnit(BigDecimal unit)
	{
		this.unit = unit;
		return this;
	}

	public FuelBillEntityBuilder setPricePerUnit(BigDecimal pricePerUnit)
	{
		this.pricePerUnit = pricePerUnit;
		return this;
	}

	public FuelBillEntityBuilder setDistance(BigDecimal distance)
	{
		this.distance = distance;
		return this;
	}

	public FuelBillEntityBuilder setEstimate(BigDecimal estimate)
	{
		this.estimate = estimate;
		return this;
	}

	public FuelBillEntityBuilder setAvgConsumption(BigDecimal avgConsumption)
	{
		this.avgConsumption = avgConsumption;
		return this;
	}

	public FuelBillEntityBuilder setCostPerKm(BigDecimal costPerKm)
	{
		this.costPerKm = costPerKm;
		return this;
	}

	public FuelBillEntity build()
	{
		validate();

		FuelBillEntity entity = new FuelBillEntity();

		entity.setDate(date);
		entity.setVatRate(vatRate);
		entity.setDistance(distance);
		entity.setEstimate(estimate);
		entity.setPricePerUnit(pricePerUnit);

		// Calculations
		calculateUnit(entity);
		calculateTotal(entity);
		calculateNet(entity);
		calculateUst(entity);
		// ToDo: average consumption
		// ToDo: cost per km

		return entity;
	}

	private void validate()
	{
		if (!atLeastTwoNotNull(unit, pricePerUnit, total))
		{
			LOG.warn("At least two of the following was empty! Total: {} - Price per unit: {} - Unit: {}", unit,
					pricePerUnit, total);
			throw new IllegalArgumentException("At least two of 'unit', 'total' or 'pricePerUnit' must be set!");
		}

		if (vatRate == null)
		{
			LOG.warn("Vat rate was not set!");
			throw new IllegalArgumentException("Vat rate cannot be null!");
		}
	}

	private void calculateTotal(FuelBillEntity entity)
	{
		if (total == null)
		{
			total = unit
					.multiply(pricePerUnit)
					.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
		}
		entity.setTotal(total);
	}

	private void calculateNet(FuelBillEntity entity)
	{
		if (netAmount == null)
		{
			netAmount = total
					.divide(vatFactor, 2, RoundingMode.HALF_UP);
		}
		entity.setNetAmount(netAmount);
	}

	private void calculateUst(FuelBillEntity entity)
	{
		if (ustAmount == null)
		{
			ustAmount = total
					.subtract(netAmount);
		}
		entity.setUstAmount(ustAmount);
	}

	private void calculateUnit(FuelBillEntity entity)
	{
		if (unit == null)
		{
			unit = total
					.divide(vatFactor, 4, RoundingMode.HALF_UP)
					.divide(pricePerUnit, 4, RoundingMode.HALF_UP)
					.setScale(2, RoundingMode.HALF_UP);
		}
		entity.setUnit(unit);
	}

	public static FuelBillEntity toEntity(FuelBillInput input)
	{
		return new FuelBillEntityBuilder()
				.setDate(input.day())
				.setVatRate(input.vatRate())
				.setDistance(input.distance())
				.setUnit(input.unit())
				.setPricePerUnit(input.pricePerUnit())
				.setTotal(input.total())
				.setEstimate(input.estimate())
				.build();
	}
}
