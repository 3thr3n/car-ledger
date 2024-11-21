package de.codeflowwizardry.carledger.data;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@SequenceGenerator(allocationSize = 1, sequenceName = "sequence_car", initialValue = 5, name = "carSequence")
public class Car
{
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "carSequence")
	private Long id;
	@Column(name = "_description")
	private String description;
	@OneToMany(mappedBy = "car")
	private final List<Bill> bills = new ArrayList<>();
	@ManyToOne
	private Account user;

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public Long getId()
	{
		return id;
	}

	public List<Bill> getBills()
	{
		return bills;
	}

	public void addBill(Bill bill)
	{
		bills.add(bill);
	}

	public Account getUser()
	{
		return user;
	}

	public void setUser(Account user)
	{
		this.user = user;
	}
}
