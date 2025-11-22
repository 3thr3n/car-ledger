package de.codeflowwizardry.carledger.data;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "Account")
@SequenceGenerator(allocationSize = 1, sequenceName = "sequence_account", initialValue = 5, name = "accountSequence")
public class AccountEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accountSequence")
	private Long id;
	@Column(name = "user_id", unique = true)
	private String userId;
	@Column(name = "max_cars")
	private int maxCars;
	@OneToMany(mappedBy = "user", targetEntity = CarEntity.class)
	private final List<CarEntity> carEntityList = new ArrayList<>();

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

	public List<CarEntity> getCarList()
	{
		return carEntityList;
	}

	public void addCar(CarEntity carEntity)
	{
		carEntityList.add(carEntity);
	}
}
