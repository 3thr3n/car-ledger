package de.codeflowwizardry.carledger.data;

import jakarta.persistence.*;

@Entity(name = "Car")
public class CarEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "c_name", nullable = false)
	private String name;
	@Column(name = "c_manufacture_year", nullable = false)
	private int manufactureYear;
	@Column(name = "c_odometer", nullable = false)
	private int odometer = 0;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "c_user_id", nullable = false)
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

	public AccountEntity getUser()
	{
		return user;
	}

	public void setUser(AccountEntity user)
	{
		this.user = user;
	}
}
