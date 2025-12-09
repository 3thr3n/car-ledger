package de.codeflowwizardry.carledger.data;

import static de.codeflowwizardry.carledger.StatsCalculator.ONE_HUNDRED;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Date;

import de.codeflowwizardry.carledger.rest.records.FuelBillInput;
import jakarta.persistence.*;

@Entity
@Table(name = "bill_fuel")
public class FuelBillEntity extends BillEntity
{
	@MapsId
	@OneToOne
	@JoinColumn(name = "id")
	private BillEntity bill;

	@Column(name = "f_unit", updatable = false)
	private BigDecimal unit = BigDecimal.ZERO;

	@Column(name = "f_price_per_unit")
	private BigDecimal pricePerUnit = BigDecimal.ZERO;

	@Column(name = "f_distance")
	private BigDecimal distance = BigDecimal.ZERO;

	@Column(name = "f_estimate")
	private BigDecimal estimate = BigDecimal.ZERO;

	@Column(name = "f_avg_consumption")
	private BigDecimal avgConsumption = BigDecimal.ZERO;

	@Column(name = "f_cost_per_km")
	private BigDecimal costPerKm = BigDecimal.ZERO;

	public FuelBillEntity()
	{
	}

	private FuelBillEntity(Long id, LocalDate day, BigDecimal distance, BigDecimal unit, BigDecimal pricePerUnit,
			BigInteger vatRate, BigDecimal totalNet, BigDecimal totalGross, BigDecimal totalUst, BigDecimal estimate,
			BigDecimal avgConsumption)
	{
		setId(id);
		setDay(day);
		setDistance(distance);
		setUnit(unit);
		setPricePerUnit(pricePerUnit);
		setVatRate(vatRate);
		setNetAmount(totalNet);
        setUstAmount(totalUst);
		setTotal(totalGross);
		setEstimate(estimate);
		setAvgConsumption(avgConsumption);
	}

	public BillEntity getBill()
	{
		return bill;
	}

	public BigDecimal getUnit()
	{
		return unit;
	}

	public void setUnit(BigDecimal unit)
	{
		this.unit = unit;
	}

	public BigDecimal getPricePerUnit()
	{
		return pricePerUnit;
	}

	public void setPricePerUnit(BigDecimal pricePerUnit)
	{
		this.pricePerUnit = pricePerUnit;
	}

	public BigDecimal getDistance()
	{
		return distance;
	}

	public boolean isDistanceSet()
	{
		return !BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).equals(distance);
	}

	public void setDistance(BigDecimal distance)
	{
		this.distance = distance;
	}

	public BigDecimal getEstimate()
	{
		return estimate;
	}

	public void setEstimate(BigDecimal estimate)
	{
		this.estimate = estimate;
	}

	public BigDecimal getAvgConsumption()
	{
		return avgConsumption;
	}

	public void setAvgConsumption(BigDecimal avgConsumption)
	{
		this.avgConsumption = avgConsumption;
	}

	public BigDecimal getCostPerKm()
	{
		return costPerKm;
	}

	public void setCostPerKm(BigDecimal costPerKm)
	{
		this.costPerKm = costPerKm;
	}

	public BigDecimal getCalculateConsumption()
	{
		if (unit.compareTo(BigDecimal.ZERO) <= 0 || distance.compareTo(BigDecimal.ZERO) <= 0)
		{
			return BigDecimal.ZERO;
		}
		return unit.divide(distance, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
	}

	public static FuelBillEntity toEntity(FuelBillInput input)
	{
        BigDecimal vatRate = new BigDecimal(input.vatRate());
        vatRate = BigDecimal.ONE.add(vatRate.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));

        BigDecimal totalRaw = input.pricePerUnit()
                .divide(vatRate, 4, RoundingMode.HALF_UP)
                .multiply(input.unit());

        BigDecimal totalNet = totalRaw
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);

        BigDecimal totalGross = totalRaw
                .multiply(vatRate)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);

        BigDecimal totalUst = totalGross.subtract(totalNet);

		var avgConsumption = input.distance().compareTo(BigDecimal.ZERO) == 0
				? null
				: input.unit().multiply(ONE_HUNDRED)
						.divide(input.distance(), RoundingMode.HALF_UP);

		return new FuelBillEntity(
				null,
				input.day(),
				input.distance(),
				input.unit(),
				input.pricePerUnit(),
                input.vatRate(),
				totalNet,
				totalGross,
                totalUst,
                input.estimate(),
				avgConsumption);
	}
}
