package de.codeflowwizardry.carledger.data;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;

/**
 * The access of another account to a car, with its role and an optional
 * period. The owner of the car isn't one, it is {@link CarEntity#getUser()}.
 * It starts as an invitation and counts once it is
 * {@link CarAccessState#ACCEPTED}.
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

	/** missing until the invited account exists, see {@link #invitee} */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ca_account_id")
	private AccountEntity account;

	/**
	 * user name or e-mail address in lower case of an account that doesn't exist
	 * yet, it gets the invitation with its first login
	 */
	@Column(name = "ca_invitee", length = 320)
	private String invitee;

	@Enumerated(EnumType.STRING)
	@Column(name = "ca_state", nullable = false, length = 10)
	private CarAccessState state = CarAccessState.PENDING;

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

	/**
	 * An access the account already has, e.g. of the time before the invitations
	 */
	public CarAccessEntity(CarEntity car, AccountEntity account, CarRole role, AccountEntity createdBy)
	{
		this(car, account, null, role, createdBy);
		this.state = CarAccessState.ACCEPTED;
	}

	/**
	 * An invitation, of an account or of one that doesn't exist yet
	 */
	public static CarAccessEntity invite(CarEntity car, AccountEntity account, String invitee, CarRole role,
			AccountEntity createdBy)
	{
		return new CarAccessEntity(car, account, account == null ? invitee : null, role, createdBy);
	}

	private CarAccessEntity(CarEntity car, AccountEntity account, String invitee, CarRole role,
			AccountEntity createdBy)
	{
		if (role == CarRole.OWNER)
		{
			throw new IllegalArgumentException("The owner is the user of the car");
		}
		if (account == null && invitee == null)
		{
			throw new IllegalArgumentException("An account or an invitee is needed");
		}
		this.car = car;
		this.account = account;
		this.invitee = invitee;
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

	public String getInvitee()
	{
		return invitee;
	}

	/**
	 * The invited account logged in for the first time
	 */
	public void claim(AccountEntity account)
	{
		this.account = account;
		this.invitee = null;
	}

	public CarAccessState getState()
	{
		return state;
	}

	public void setState(CarAccessState state)
	{
		this.state = state;
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
