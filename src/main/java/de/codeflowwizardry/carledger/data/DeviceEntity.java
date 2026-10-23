package de.codeflowwizardry.carledger.data;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;

/**
 * A GPS tracker of a car, it sends its track points with its own token
 */
@Entity(name = "device")
public class DeviceEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(insertable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "dv_car_id", nullable = false, updatable = false)
	private CarEntity car;

	@Column(name = "dv_name", nullable = false, length = 100)
	private String name;

	/** SHA-256 of the token, hex */
	@Column(name = "dv_token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "dv_created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

	@Column(name = "dv_last_seen_at")
	private OffsetDateTime lastSeenAt;

	@Column(name = "dv_revoked_at")
	private OffsetDateTime revokedAt;

	protected DeviceEntity()
	{
	}

	public DeviceEntity(CarEntity car, String name, String tokenHash)
	{
		this.car = car;
		this.name = name;
		this.tokenHash = tokenHash;
	}

	/**
	 * A tracker of a backup, with its times
	 */
	public DeviceEntity(CarEntity car, String name, String tokenHash, OffsetDateTime createdAt,
			OffsetDateTime lastSeenAt, OffsetDateTime revokedAt)
	{
		this(car, name, tokenHash);
		this.createdAt = createdAt;
		this.lastSeenAt = lastSeenAt;
		this.revokedAt = revokedAt;
	}

	public Long getId()
	{
		return id;
	}

	public CarEntity getCar()
	{
		return car;
	}

	public String getName()
	{
		return name;
	}

	public String getTokenHash()
	{
		return tokenHash;
	}

	public OffsetDateTime getCreatedAt()
	{
		return createdAt;
	}

	public OffsetDateTime getLastSeenAt()
	{
		return lastSeenAt;
	}

	public OffsetDateTime getRevokedAt()
	{
		return revokedAt;
	}

	/**
	 * A new token, the old one stops working
	 */
	public void setTokenHash(String tokenHash)
	{
		this.tokenHash = tokenHash;
	}

	public void revoke()
	{
		if (revokedAt == null)
		{
			// as precise as stored, the answer of the revoke shows the time of the list
			revokedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
		}
	}
}
