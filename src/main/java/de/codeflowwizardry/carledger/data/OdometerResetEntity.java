package de.codeflowwizardry.carledger.data;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

/**
 * An odometer reset of the trip log: a replaced instrument cluster or a reset
 * odometer. From its time on the odometer counts from the new reading.
 * Append-only like a trip (a trigger of the database guards it): it is deleted
 * once with a reason, the row stays. So there is no setter.
 */
@Entity(name = "odometer_reset")
public class OdometerResetEntity
{
	/** created by the client */
	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rs_car_id", nullable = false, updatable = false)
	private CarEntity car;

	@Column(name = "rs_reset_at", nullable = false, updatable = false)
	private OffsetDateTime resetAt;

	@Column(name = "rs_odometer", nullable = false, updatable = false)
	private int odometer;

	@Column(name = "rs_odometer_before", updatable = false)
	private Integer odometerBefore;

	@Column(name = "rs_reason", nullable = false, updatable = false, length = 500)
	private String reason;

	/** the user id of the account, no reference: the row stays without it */
	@Column(name = "rs_created_by", nullable = false, updatable = false, length = 255)
	private String createdBy;

	@Column(name = "rs_restored", nullable = false, updatable = false)
	private boolean restored;

	@Column(name = "rs_created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "rs_deleted_at")
	private OffsetDateTime deletedAt;

	@Column(name = "rs_deleted_by", length = 255)
	private String deletedBy;

	@Column(name = "rs_delete_reason", length = 500)
	private String deleteReason;

	protected OdometerResetEntity()
	{
	}

	/**
	 * @param odometer
	 *            the new reading
	 * @param odometerBefore
	 *            the last reading before, if it was still shown
	 * @param createdBy
	 *            user id of the account
	 * @param createdAt
	 *            when the server received it
	 */
	public OdometerResetEntity(UUID id, CarEntity car, OffsetDateTime resetAt, int odometer, Integer odometerBefore,
			String reason, String createdBy, boolean restored, OffsetDateTime createdAt)
	{
		this.id = id;
		this.car = car;
		this.resetAt = resetAt;
		this.odometer = odometer;
		this.odometerBefore = odometerBefore;
		this.reason = reason;
		this.createdBy = createdBy;
		this.restored = restored;
		this.createdAt = createdAt;
	}

	/**
	 * Deleted resets stay listed, they don't count for the odometer
	 *
	 * @param deletedBy
	 *            user id of the account
	 */
	public void delete(OffsetDateTime deletedAt, String deletedBy, String deleteReason)
	{
		this.deletedAt = deletedAt;
		this.deletedBy = deletedBy;
		this.deleteReason = deleteReason;
	}

	public boolean isDeleted()
	{
		return deletedAt != null;
	}

	public UUID getId()
	{
		return id;
	}

	public CarEntity getCar()
	{
		return car;
	}

	public OffsetDateTime getResetAt()
	{
		return resetAt;
	}

	public int getOdometer()
	{
		return odometer;
	}

	public Integer getOdometerBefore()
	{
		return odometerBefore;
	}

	public String getReason()
	{
		return reason;
	}

	public String getCreatedBy()
	{
		return createdBy;
	}

	public boolean isRestored()
	{
		return restored;
	}

	public OffsetDateTime getCreatedAt()
	{
		return createdAt;
	}

	public OffsetDateTime getDeletedAt()
	{
		return deletedAt;
	}

	public String getDeletedBy()
	{
		return deletedBy;
	}

	public String getDeleteReason()
	{
		return deleteReason;
	}
}
