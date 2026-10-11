package de.codeflowwizardry.carledger.data;

import jakarta.persistence.*;

@Entity(name = "Account")
public class AccountEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "user_id", nullable = false, unique = true)
	private String userId;
	@Column(name = "max_cars", nullable = false)
	private int maxCars;

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
}
