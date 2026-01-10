package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

import de.codeflowwizardry.carledger.data.MaintenanceBillEntity;

public final class MaintenanceBill extends AbstractBill<MaintenanceBillEntity>
{
	private final Long id;
	private final LocalDate date;
	private final int year;
	private final String workshop;
	private final String description;
	private final BigDecimal total;
	private final BigDecimal laborCost;
	private final BigDecimal partsCost;
	private final BigInteger odometer;

	MaintenanceBill(Long id, LocalDate date, String workshop, String description, BigDecimal total,
			BigDecimal laborCost, BigDecimal partsCost, BigInteger odometer)
	{
		this.id = id;
		this.date = date;
		this.year = date.getYear();
		this.workshop = workshop;
		this.description = description;
		this.total = total;
		this.laborCost = laborCost;
		this.partsCost = partsCost;
		this.odometer = odometer;
	}

	public static MaintenanceBill convert(MaintenanceBillEntity billEntity)
	{
		return new MaintenanceBill(billEntity.getBill().getId(), billEntity.getBill().getDate(),
				billEntity.getWorkshop(), billEntity.getDescription(), billEntity.getBill().getTotal(),
				billEntity.getLaborCost(), billEntity.getPartsCost(), billEntity.getOdometer());
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

	public String getWorkshop()
	{
		return workshop;
	}

	public String getDescription()
	{
		return description;
	}

	public BigDecimal getTotal()
	{
		return total;
	}

	public BigDecimal getLaborCost()
	{
		return laborCost;
	}

	public BigDecimal getPartsCost()
	{
		return partsCost;
	}

	public BigInteger getOdometer()
	{
		return odometer;
	}
}
