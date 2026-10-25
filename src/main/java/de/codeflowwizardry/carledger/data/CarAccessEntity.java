package de.codeflowwizardry.carledger.data;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;

/**
 * The access of another account to a car, with its role and an optional
 * period. The owner of the car isn't one, it is {@link CarEntity#getUser()}.
 */
@Entity(name = "car_access")
public class CarAccessEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(insertable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ca_car_id", nullable = false, updatable = false)
	private CarEntity car;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ca_account_id", nullable = false, updatable = false)
	private AccountEntity account;

	/** never {@link CarRole#OWNER} */
	@Enumerated(EnumType.STRING)
	@Column(name = "ca_role", nullable = false, length = 10)
	private CarRole role;

	/** inclusive, from now on without */
	@Column(name = "ca_valid_from")
	private OffsetDateTime validFrom;

	/** exclusive, without an end without */
	@Column(name = "ca_valid_until")
	private OffsetDateTime validUntil;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ca_created_by", updatable = false)
	private AccountEntity createdBy;

	@Column(name = "ca_created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

	protected CarAccessEntity()
	{
	}

	public CarAccessEntity(CarEntity car, AccountEntity account, CarRole role, AccountEntity createdBy)
	{
		if (role == CarRole.OWNER)
		{
			throw new IllegalArgumentException("The owner is the user of the car");
		}
		this.car = car;
		this.account = account;
		this.role = role;
		this.createdBy = createdBy;
	}

	public Long getId()
	{
		return id;
	}

	public CarEntity getCar()
	{
		return car;
	}

	public AccountEntity getAccount()
	{
		return account;
	}

	public CarRole getRole()
	{
		return role;
	}

	public void setRole(CarRole role)
	{
		if (role == CarRole.OWNER)
		{
			throw new IllegalArgumentException("The owner is the user of the car");
		}
		this.role = role;
	}

	public OffsetDateTime getValidFrom()
	{
		return validFrom;
	}

	public void setValidFrom(OffsetDateTime validFrom)
	{
		this.validFrom = validFrom;
	}

	public OffsetDateTime getValidUntil()
	{
		return validUntil;
	}

	public void setValidUntil(OffsetDateTime validUntil)
	{
		this.validUntil = validUntil;
	}

	public AccountEntity getCreatedBy()
	{
		return createdBy;
	}

	public OffsetDateTime getCreatedAt()
	{
		return createdAt;
	}
}
