package de.codeflowwizardry.carledger.data;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.ws.rs.DefaultValue;

@Entity(name = "Car")
@SequenceGenerator(allocationSize = 1, sequenceName = "sequence_car", initialValue = 5, name = "carSequence")
public class CarEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "carSequence")
	private Long id;
	@Column(name = "c_name")
	private String name;
	@Column(name = "c_manufacture_year")
	private int manufactureYear;
	@DefaultValue("0")
	@Column(name = "c_odometer", nullable = false)
	private int odometer = 0;

	@OneToMany(mappedBy = "car")
	private final List<BillEntity> billEntities = new ArrayList<>();

	@OneToMany(mappedBy = "car")
	private final List<RecurringBillEntity> recurringBillEntities = new ArrayList<>();

	@ManyToOne
	private AccountEntity user;

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

	public List<BillEntity> getBills()
	{
		return billEntities;
	}

	public void addBill(BillEntity billEntity)
	{
		billEntities.add(billEntity);
	}

	public List<RecurringBillEntity> getRecurringBillEntities()
	{
		return recurringBillEntities;
	}

	public AccountEntity getUser()
	{
		return user;
	}

	public void setUser(AccountEntity user)
	{
		this.user = user;
	}
}
