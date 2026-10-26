package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;

public class Bill extends AbstractBill
{
	private final Long id;
	private final LocalDate date;
	private final int year;
	private final BigDecimal total;
	private final int vatRate;
	private final BigDecimal ustAmount;
	private final BigDecimal netAmount;
	private final BillType type;

	Bill(Long id, LocalDate date, int year, BigDecimal total, int vatRate, BigDecimal ustAmount,
			BigDecimal netAmount, BillType type)
	{
		this.id = id;
		this.date = date;
		this.year = year;
		this.total = total;
		this.vatRate = vatRate;
		this.ustAmount = ustAmount;
		this.netAmount = netAmount;
		this.type = type;
	}

	public static Bill convert(BillEntity entity)
	{
		return created(new Bill(entity.getId(), entity.getDate(), entity.getDate().getYear(), entity.getTotal(),
				entity.getVatRate(), entity.getUstAmount(), entity.getNetAmount(), entity.getType()), entity);
	}

	public static List<Bill> convert(List<BillEntity> entityList)
	{
		return entityList.stream()
				.map(Bill::convert)
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

	public BigDecimal getTotal()
	{
		return total;
	}

	public int getVatRate()
	{
		return vatRate;
	}

	public BigDecimal getUstAmount()
	{
		return ustAmount;
	}

	public BigDecimal getNetAmount()
	{
		return netAmount;
	}

	public BillType getType()
	{
		return type;
	}
}
