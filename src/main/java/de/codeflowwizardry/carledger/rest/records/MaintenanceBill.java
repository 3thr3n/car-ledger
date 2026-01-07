package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import de.codeflowwizardry.carledger.data.MaintenanceBillEntity;

public final class MaintenanceBill extends AbstractBill<MaintenanceBillEntity>
{
	private final Long id;
	private final LocalDate date;
	private final int year;
	private final BigDecimal distance;
	private final BigDecimal unit;
	private final BigDecimal pricePerUnit;
	private final BigDecimal estimate;
	private final BigDecimal calculated;
	private final BigDecimal calculatedPrice;

	MaintenanceBill(Long id, LocalDate date, BigDecimal distance, BigDecimal unit, BigDecimal pricePerUnit,
			BigDecimal estimate,
			BigDecimal calculated, BigDecimal calculatedPrice)
	{
		this.id = id;
		this.date = date;
		this.year = date.getYear();
		this.distance = Objects.requireNonNullElse(distance, BigDecimal.ZERO);
		this.unit = unit;
		this.pricePerUnit = pricePerUnit;
		this.estimate = estimate;
		this.calculated = Objects.requireNonNullElse(calculated, BigDecimal.ZERO);
		this.calculatedPrice = Objects.requireNonNullElse(calculatedPrice, BigDecimal.ZERO);
	}

	public static MaintenanceBill convert(MaintenanceBillEntity billEntity)
	{
		return new MaintenanceBill(null, LocalDate.now(), null, null, null, null, null, null);
	}

	public static List<MaintenanceBill> convert(List<MaintenanceBillEntity> billEntityList)
	{
		return billEntityList.stream()
				.map(MaintenanceBill::convert)
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

	public String getEstimate()
	{
		return estimate.setScale(2, RoundingMode.HALF_UP).toString();
	}

	public String getCalculated()
	{
		return calculated.setScale(2, RoundingMode.HALF_UP).toString();
	}

	public String getCalculatedPrice()
	{
		return calculatedPrice.setScale(2, RoundingMode.HALF_UP).toString();
	}
}
