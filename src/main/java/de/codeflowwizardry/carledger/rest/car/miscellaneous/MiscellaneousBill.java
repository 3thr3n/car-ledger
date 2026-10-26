package de.codeflowwizardry.carledger.rest.car.miscellaneous;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import de.codeflowwizardry.carledger.data.MiscellaneousBillEntity;
import de.codeflowwizardry.carledger.rest.records.AbstractBill;

public final class MiscellaneousBill extends AbstractBill
{
	private final Long id;
	private final LocalDate date;
	private final int year;
	private final int vatRate;
	private final String description;
	private final BigDecimal total;

	MiscellaneousBill(Long id, LocalDate date, String description, BigDecimal total, int vatRate)
	{
		this.vatRate = vatRate;
		this.id = id;
		this.date = date;
		this.year = date.getYear();
		this.description = description;
		this.total = total;
	}

	public static MiscellaneousBill convert(MiscellaneousBillEntity billEntity)
	{
		return created(new MiscellaneousBill(billEntity.getBill().getId(), billEntity.getBill().getDate(),
				billEntity.getDescription(), billEntity.getBill().getTotal(),
				billEntity.getBill().getVatRate()), billEntity.getBill());
	}

	public static List<MiscellaneousBill> convert(List<MiscellaneousBillEntity> billEntityList)
	{
		return billEntityList.stream()
				.map(MiscellaneousBill::convert)
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

	public int getVatRate()
	{
		return vatRate;
	}

	public String getDescription()
	{
		return description;
	}

	public BigDecimal getTotal()
	{
		return total;
	}
}
