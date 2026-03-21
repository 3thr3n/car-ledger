package de.codeflowwizardry.carledger.rest.car.fuel;

import static java.util.Objects.requireNonNullElse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.rest.records.AbstractBill;

public final class FuelBill extends AbstractBill<FuelBillEntity>
{
	private final Long id;
	private final LocalDate date;
	private final int year;
	private final BigDecimal distance;
	private final BigDecimal unit;
	private final BigDecimal pricePerUnit;
	private final BigDecimal estimateConsumption;
	private final BigDecimal avgConsumption;
	private final BigDecimal total;

	FuelBill(Long id, LocalDate date, BigDecimal distance, BigDecimal unit, BigDecimal pricePerUnit,
			BigDecimal estimateConsumption,
			BigDecimal avgConsumption, BigDecimal total)
	{
		this.id = id;
		this.date = date;
		this.year = date.getYear();
		this.distance = requireNonNullElse(distance, BigDecimal.ZERO);
		this.unit = unit;
		this.pricePerUnit = pricePerUnit;
		this.estimateConsumption = estimateConsumption;
		this.avgConsumption = requireNonNullElse(avgConsumption, BigDecimal.ZERO);
		this.total = requireNonNullElse(total, BigDecimal.ZERO);
	}

	public static FuelBill convert(FuelBillEntity billEntity)
	{
		return new FuelBill(billEntity.getBill().getId(),
				billEntity.getBill().getDate(),
				billEntity.getDistance(),
				billEntity.getUnit(),
				billEntity.getPricePerUnit(),
				billEntity.getEstimate(),
				billEntity.getAvgConsumption(),
				billEntity.getBill().getTotal());
	}

	public static List<FuelBill> convert(List<FuelBillEntity> billEntityList)
	{
		return billEntityList.stream()
				.map(FuelBill::convert)
				.toList();
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

	public String getDistance()
	{
		return distance.setScale(2, RoundingMode.HALF_UP).toString();
	}

	public String getUnit()
	{
		return unit.setScale(2, RoundingMode.HALF_UP).toString();
	}

	public String getPricePerUnit()
	{
		return pricePerUnit.setScale(2, RoundingMode.HALF_UP).toString();
	}

	public String getEstimateConsumption()
	{
		return estimateConsumption.setScale(2, RoundingMode.HALF_UP).toString();
	}

	public String getAvgConsumption()
	{
		return avgConsumption.setScale(2, RoundingMode.HALF_UP).toString();
	}

	public String getTotal()
	{
		return total.setScale(2, RoundingMode.HALF_UP).toString();
	}
}
