package de.codeflowwizardry.carledger.rest.car.charging;

import static java.util.Objects.requireNonNullElse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import de.codeflowwizardry.carledger.data.ChargingBillEntity;
import de.codeflowwizardry.carledger.data.ChargingLocation;
import de.codeflowwizardry.carledger.rest.records.AbstractBill;

/**
 * A charge in kWh, the consumption in kWh per 100 km
 */
public final class ChargingBill extends AbstractBill
{
	private final Long id;
	private final LocalDate date;
	private final int year;
	private final int vatRate;
	private final BigDecimal distance;
	private final BigDecimal unit;
	private final BigDecimal pricePerUnit;
	private final BigDecimal avgConsumption;
	private final BigDecimal total;
	private final ChargingLocation location;
	private final Integer stateOfCharge;

	ChargingBill(Long id, LocalDate date, BigDecimal distance, BigDecimal unit, BigDecimal pricePerUnit,
			BigDecimal avgConsumption, BigDecimal total, int vatRate, ChargingLocation location,
			Integer stateOfCharge)
	{
		this.id = id;
		this.date = date;
		this.year = date.getYear();
		this.vatRate = vatRate;
		this.distance = requireNonNullElse(distance, BigDecimal.ZERO);
		this.unit = requireNonNullElse(unit, BigDecimal.ZERO);
		this.pricePerUnit = requireNonNullElse(pricePerUnit, BigDecimal.ZERO);
		this.avgConsumption = requireNonNullElse(avgConsumption, BigDecimal.ZERO);
		this.total = requireNonNullElse(total, BigDecimal.ZERO);
		this.location = location;
		this.stateOfCharge = stateOfCharge;
	}

	public static ChargingBill convert(ChargingBillEntity entity)
	{
		return created(new ChargingBill(entity.getBill().getId(),
				entity.getBill().getDate(),
				entity.getDistance(),
				entity.getUnit(),
				entity.getPricePerUnit(),
				entity.getAvgConsumption(),
				entity.getBill().getTotal(),
				entity.getBill().getVatRate(),
				entity.getLocation(),
				entity.getStateOfCharge()), entity.getBill());
	}

	public static List<ChargingBill> convert(List<ChargingBillEntity> entities)
	{
		return entities.stream().map(ChargingBill::convert).toList();
	}

	public Long getId()
	{
		return id;
	}

	public LocalDate getDate()
	{
		return date;
	}

	public int getYear()
	{
		return year;
	}

	public int getVatRate()
	{
		return vatRate;
	}

	/**
	 * @return km since the last charge
	 */
	public BigDecimal getDistance()
	{
		return distance.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * @return kWh
	 */
	public BigDecimal getUnit()
	{
		return unit.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * @return cent per kWh
	 */
	public BigDecimal getPricePerUnit()
	{
		return pricePerUnit.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * @return kWh per 100 km
	 */
	public BigDecimal getAvgConsumption()
	{
		return avgConsumption.setScale(2, RoundingMode.HALF_UP);
	}

	public BigDecimal getTotal()
	{
		return total.setScale(2, RoundingMode.HALF_UP);
	}

	public ChargingLocation getLocation()
	{
		return location;
	}

	/**
	 * @return percent after charging
	 */
	public Integer getStateOfCharge()
	{
		return stateOfCharge;
	}
}
