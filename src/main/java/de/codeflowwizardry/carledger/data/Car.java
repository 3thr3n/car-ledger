package de.codeflowwizardry.carledger.data;

import jakarta.persistence.*;
import jakarta.ws.rs.DefaultValue;

import java.util.ArrayList;
import java.util.List;

@Entity
@SequenceGenerator(allocationSize = 1, sequenceName = "sequence_car", initialValue = 5, name = "carSequence")
public class Car
{
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "carSequence")
	private Long id;
	@Column(name = "_name")
	private String name;
	@Column(name = "_manufacture_year")
	private int manufactureYear;
    @DefaultValue("0")
	@Column(name = "_odometer", nullable = false)
	private int odometer;
	@OneToMany(mappedBy = "car")
	private final List<Bill> bills = new ArrayList<>();
	@ManyToOne
	private Account user;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public int getManufactureYear()
	{
		return manufactureYear;
	}

	public void setManufactureYear(int manufactureYear)
	{
		this.manufactureYear = manufactureYear;
	}

	public int getOdometer()
	{
		return odometer;
	}

	public void setOdometer(int odometer)
	{
		this.odometer = odometer;
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
