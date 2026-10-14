package de.codeflowwizardry.carledger.data;

import static de.codeflowwizardry.carledger.Utils.valueWasSet;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "bill_charging")
public class ChargingBillEntity extends AbstractBillEntity implements EnergyBill
{
	@Id
	private Long id;

	@MapsId
	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "bill_id")
	private BillEntity bill;

	@Column(name = "c_unit", nullable = false)
	private BigDecimal unit = BigDecimal.ZERO;

	@Column(name = "c_price_per_unit", nullable = false)
	private BigDecimal pricePerUnit = BigDecimal.ZERO;

	@Column(name = "c_distance")
	private BigDecimal distance = BigDecimal.ZERO;

	@Column(name = "c_avg_consumption")
	private BigDecimal avgConsumption = BigDecimal.ZERO;

	@Column(name = "c_cost_per_km")
	private BigDecimal costPerKm = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(name = "c_location")
	private ChargingLocation location;

	/** in percent, after charging */
	@Column(name = "c_state_of_charge")
	private Integer stateOfCharge;

	protected ChargingBillEntity()
	{
	}

	public ChargingBillEntity(BillEntity bill)
	{
		this.bill = bill;
	}

	@Override
	public BillEntity getBill()
	{
		return bill;
	}

	/**
	 * @return in kWh
	 */
	@Override
	public BigDecimal getUnit()
	{
		return unit;
	}

	public void setUnit(BigDecimal unit)
	{
		this.unit = unit;
	}

	/**
	 * @return in cent per kWh
	 */
	@Override
	public BigDecimal getPricePerUnit()
	{
		return pricePerUnit;
	}

	public void setPricePerUnit(BigDecimal pricePerUnit)
	{
		this.pricePerUnit = pricePerUnit;
	}

	/**
	 * @return in km since the last charge
	 */
	@Override
	public BigDecimal getDistance()
	{
		return distance;
	}

	@Override
	public boolean isDistanceMissing()
	{
		return !valueWasSet(distance);
	}

	public void setDistance(BigDecimal distance)
	{
		this.distance = distance;
	}

	/**
	 * @return in kWh per 100 km
	 */
	public BigDecimal getAvgConsumption()
	{
		return avgConsumption;
	}

	@Override
	public void setAvgConsumption(BigDecimal avgConsumption)
	{
		this.avgConsumption = avgConsumption;
	}

	/**
	 * @return in cent
	 */
	public BigDecimal getCostPerKm()
	{
		return costPerKm;
	}

	@Override
	public void setCostPerKm(BigDecimal costPerKm)
	{
		this.costPerKm = costPerKm;
	}

	public ChargingLocation getLocation()
	{
		return location;
	}

	public void setLocation(ChargingLocation location)
	{
		this.location = location;
	}

	public Integer getStateOfCharge()
	{
		return stateOfCharge;
	}

	public void setStateOfCharge(Integer stateOfCharge)
	{
		this.stateOfCharge = stateOfCharge;
	}
}
