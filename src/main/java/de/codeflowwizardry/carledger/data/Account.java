package de.codeflowwizardry.carledger.data;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@SequenceGenerator(allocationSize = 1, sequenceName = "sequence_account", initialValue = 5, name = "accountSequence")
public class Account
{
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accountSequence")
	private Long id;
	@Column(name = "user_id", unique = true)
	private String userId;
	@Column(name = "max_cars")
	private int maxCars;
	@OneToMany(mappedBy = "user", targetEntity = Car.class)
	private final List<Car> carList = new ArrayList<>();

	public Long getId()
	{
		return id;
	}

	public String getUserId()
	{
		return userId;
	}

	public void setUserId(String userId)
	{
		this.userId = userId;
	}

	public int getMaxCars()
	{
		return maxCars;
	}

	public void setMaxCars(int maxCars)
	{
		this.maxCars = maxCars;
	}

	public List<Car> getCarList()
	{
		return carList;
	}

	public void addCar(Car car)
	{
		carList.add(car);
	}
}
