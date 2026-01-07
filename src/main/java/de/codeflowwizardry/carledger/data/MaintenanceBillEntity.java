package de.codeflowwizardry.carledger.data;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.persistence.*;

@Entity
@Table(name = "bill_maintenance")
public class MaintenanceBillEntity extends AbstractBillEntity
{
	@Id
	private Long id;

	@MapsId
	@OneToOne
	@JoinColumn(name = "bill_id")
	private BillEntity bill;

	@Column(name = "m_description", length = Integer.MAX_VALUE)
	private String description;

	@Column(name = "m_workshop", length = Integer.MAX_VALUE)
	private String workshop;

	@Column(name = "m_labor_cost")
	private BigDecimal laborCost;
	@Column(name = "m_parts_cost")
	private BigDecimal partsCost;

	@Column(name = "m_odometer")
	private BigInteger odometer;

	protected MaintenanceBillEntity()
	{
	}

	public MaintenanceBillEntity(BillEntity bill)
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

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getWorkshop()
	{
		return workshop;
	}

	public void setWorkshop(String workshop)
	{
		this.workshop = workshop;
	}

	public BigDecimal getLaborCost()
	{
		return laborCost;
	}

	public void setLaborCost(BigDecimal laborCost)
	{
		this.laborCost = laborCost;
	}

	public BigDecimal getPartsCost()
	{
		return partsCost;
	}

	public void setPartsCost(BigDecimal partsCost)
	{
		this.partsCost = partsCost;
	}

	public BigInteger getOdometer()
	{
		return odometer;
	}

	public void setOdometer(BigInteger odometer)
	{
		this.odometer = odometer;
	}
}
