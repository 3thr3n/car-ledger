package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import de.codeflowwizardry.carledger.data.Bill;

public final class BillPojo
{
	private final Long id;
	private final LocalDate day;
	private final BigDecimal distance;
	private final BigDecimal unit;
	private final BigDecimal pricePerUnit;
	private final BigDecimal estimate;
	private final BigDecimal calculated;
	private final BigDecimal calculatedPrice;

	BillPojo(Long id, LocalDate day, BigDecimal distance, BigDecimal unit, BigDecimal pricePerUnit, BigDecimal estimate,
			BigDecimal calculated, BigDecimal calculatedPrice)
	{
		this.id = id;
		this.day = day;
		this.distance = Objects.requireNonNullElse(distance, BigDecimal.ZERO);
		this.unit = unit;
		this.pricePerUnit = pricePerUnit;
		this.estimate = estimate;
		this.calculated = Objects.requireNonNullElse(calculated, BigDecimal.ZERO);
		this.calculatedPrice = Objects.requireNonNullElse(calculatedPrice, BigDecimal.ZERO);
	}

	public static BillPojo convert(Bill bill)
	{
		return new BillPojoBuilder().setId(bill.getId()).setDay(bill.getDay()).setDistance(bill.getDistance())
				.setUnit(bill.getUnit()).setPricePerUnit(bill.getPricePerUnit()).setEstimate(bill.getEstimate())
				.setCalculated(calculateConsumption(bill.getUnit(), bill.getDistance()))
				.setCalculatedPrice(bill.getCalculatedPrice(BigDecimal.valueOf(1.19))).createBillPojo();
	}

	public static List<BillPojo> convert(List<Bill> billList)
	{
		return billList.stream()
				.map(t -> new BillPojoBuilder().setId(t.getId()).setDay(t.getDay()).setDistance(t.getDistance())
						.setUnit(t.getUnit()).setPricePerUnit(t.getPricePerUnit()).setEstimate(t.getEstimate())
						.setCalculated(calculateConsumption(t.getUnit(), t.getDistance()))
						.setCalculatedPrice(t.getCalculatedPrice(BigDecimal.valueOf(1.19))).createBillPojo())
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
