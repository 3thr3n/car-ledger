package de.codeflowwizardry.carledger.data;

import static de.codeflowwizardry.carledger.StatsCalculator.ONE_HUNDRED;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;

import de.codeflowwizardry.carledger.builders.FuelBillEntityBuilder;
import de.codeflowwizardry.carledger.rest.records.FuelBillBuilder;
import de.codeflowwizardry.carledger.rest.records.FuelBillInput;
import jakarta.persistence.*;
import jakarta.ws.rs.WebApplicationException;

@Entity
@Table(name = "bill_fuel")
public class FuelBillEntity extends BillEntity
{
	@MapsId
	@OneToOne
	@JoinColumn(name = "bill_id")
	private BillEntity bill;

	@Column(name = "f_unit", updatable = false, nullable = false)
	private BigDecimal unit = BigDecimal.ZERO;

	@Column(name = "f_price_per_unit", nullable = false)
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
		setType(BillType.FUEL);
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

	static FuelBillEntity toEntity(FuelBillInput input)
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

//
//		BigDecimal totalRaw;
//		BigDecimal totalNet;
//		BigDecimal totalGross;
//

//
//		totalNet = totalRaw
//				.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
//
//		totalGross = totalRaw
//				.multiply(vatRate)
//				.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
//
//		BigDecimal totalUst = totalGross.subtract(totalNet);
//
//		var avgConsumption = input.calculateAverageConsumption();
//
//		return new FuelBillEntity(
//				null,
//				input.day(),
//				input.distance(),
//				input.unit(),
//				input.pricePerUnit(),
//				input.vatRate(),
//				totalNet,
//				totalGross,
//				totalUst,
//				input.estimate(),
//				avgConsumption);
	}
}
