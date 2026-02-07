package de.codeflowwizardry.carledger.data;

import jakarta.persistence.*;

@Entity
@Table(name = "bill_miscellaneous")
public class MiscellaneousBillEntity extends AbstractBillEntity
{
	@Id
	private Long id;

	@MapsId
	@OneToOne
	@JoinColumn(name = "bill_id")
	private BillEntity bill;

	@Column(name = "m_description", length = Integer.MAX_VALUE)
	private String description;

	protected MiscellaneousBillEntity()
	{
	}

	public MiscellaneousBillEntity(BillEntity bill)
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
}
