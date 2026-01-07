package de.codeflowwizardry.carledger.data;

import static de.codeflowwizardry.carledger.Utils.valueWasSet;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "bill_fuel")
public class FuelBillEntity extends AbstractBillEntity
{
	@Id
	private Long id;

	@MapsId
	@OneToOne(cascade = CascadeType.ALL)
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

	protected FuelBillEntity()
	{
	}

	public FuelBillEntity(BillEntity bill)
	{
		this.bill = bill;
	}

	@Override
	public BillEntity getBill()
	{
		return bill;
	}

	@Override
	public BillType getType()
	{
		return bill.getType();
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
		return valueWasSet(distance);
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
}
