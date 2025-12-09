package de.codeflowwizardry.carledger.rest.records;

import static de.codeflowwizardry.carledger.data.BillEntity.GERMAN_UST;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.FuelBillEntity;

public final class FuelBill
{
	private final Long id;
	private final LocalDate day;
	private final int year;
	private final BigDecimal distance;
	private final BigDecimal unit;
	private final BigDecimal pricePerUnit;
	private final BigDecimal estimate;
	private final BigDecimal calculated;
	private final BigDecimal calculatedPrice;

	FuelBill(Long id, LocalDate day, BigDecimal distance, BigDecimal unit, BigDecimal pricePerUnit, BigDecimal estimate,
             BigDecimal calculated, BigDecimal calculatedPrice)
	{
		this.id = id;
		this.day = day;
		this.year = day.getYear();
		this.distance = Objects.requireNonNullElse(distance, BigDecimal.ZERO);
		this.unit = unit;
		this.pricePerUnit = pricePerUnit;
		this.estimate = estimate;
		this.calculated = Objects.requireNonNullElse(calculated, BigDecimal.ZERO);
		this.calculatedPrice = Objects.requireNonNullElse(calculatedPrice, BigDecimal.ZERO);
	}

	public static FuelBill convert(FuelBillEntity billEntity)
	{
		return new FuelBillBuilder()
                .setId(billEntity.getId())
                .setDay(billEntity.getDay())
				.setDistance(billEntity.getDistance())
				.setUnit(billEntity.getUnit()).setPricePerUnit(billEntity.getPricePerUnit())
				.setEstimate(billEntity.getEstimate())
				.setCalculated(billEntity.getAvgConsumption())
				.setCalculatedPrice(billEntity.getTotal())
                .createBillPojo();
	}

	public static List<FuelBill> convert(List<FuelBillEntity> billEntityList)
	{
		return billEntityList.stream()
				.map(t -> new FuelBillBuilder()
                        .setId(t.getId())
                        .setDay(t.getDay())
                        .setDistance(t.getDistance())
						.setUnit(t.getUnit())
                        .setPricePerUnit(t.getPricePerUnit())
                        .setEstimate(t.getEstimate())
						.setCalculated(t.getAvgConsumption())
						.setCalculatedPrice(t.getTotal())
                        .createBillPojo())
				.toList();
	}

	static BigDecimal calculateConsumption(BigDecimal unit, BigDecimal distance)
	{
		if (unit.compareTo(BigDecimal.ZERO) <= 0 || distance.compareTo(BigDecimal.ZERO) <= 0)
		{
			return BigDecimal.ZERO;
		}
		return unit.divide(distance, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
	}

	public Long getId()
	{
		return id;
	}

	public LocalDate getDay()
	{
		return day;
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
