package de.codeflowwizardry.carledger.data;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

/**
 * A trip of the trip log. Append-only (a trigger of the database guards it):
 * the start never changes, the finish is set once, the details can only be
 * added, and a trip is deleted once with a reason, the row stays. So there are
 * setters only for these.
 */
@Entity(name = "trip")
public class TripEntity
{
	/** created by the client */
	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tp_car_id", nullable = false, updatable = false)
	private CarEntity car;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tp_driver_id", nullable = false, updatable = false)
	private AccountEntity driver;

	@Enumerated(EnumType.STRING)
	@Column(name = "tp_type", nullable = false, updatable = false, length = 10)
	private TripType type;

	@Column(name = "tp_start_odometer", nullable = false, updatable = false)
	private int startOdometer;

	@Column(name = "tp_end_odometer")
	private Integer endOdometer;

	@Column(name = "tp_started_at", nullable = false, updatable = false)
	private OffsetDateTime startedAt;

	@Column(name = "tp_ended_at")
	private OffsetDateTime endedAt;

	@Column(name = "tp_destination", length = 200)
	private String destination;

	@Column(name = "tp_purpose", length = 500)
	private String purpose;

	@Column(name = "tp_business_partner", length = 200)
	private String businessPartner;

	@Enumerated(EnumType.STRING)
	@Column(name = "tp_review_reason", length = 30)
	private TripReview reviewReason;

	@Column(name = "tp_restored", nullable = false, updatable = false)
	private boolean restored;

	@Column(name = "tp_created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "tp_finished_at")
	private OffsetDateTime finishedAt;

	@Column(name = "tp_deleted_at")
	private OffsetDateTime deletedAt;

	/** the user id of the account, no reference: the row stays without it */
	@Column(name = "tp_deleted_by", length = 255)
	private String deletedBy;

	@Column(name = "tp_delete_reason", length = 500)
	private String deleteReason;

	protected TripEntity()
	{
	}

	/**
	 * @param createdAt
	 *            when the server received the start
	 */
	public TripEntity(UUID id, CarEntity car, AccountEntity driver, TripType type, int startOdometer,
			OffsetDateTime startedAt, boolean restored, OffsetDateTime createdAt)
	{
		this.id = id;
		this.car = car;
		this.driver = driver;
		this.type = type;
		this.startOdometer = startOdometer;
		this.startedAt = startedAt;
		this.restored = restored;
		this.createdAt = createdAt;
	}

	/**
	 * @param finishedAt
	 *            when the server received the finish
	 */
	public void finish(int endOdometer, OffsetDateTime endedAt, OffsetDateTime finishedAt)
	{
		this.endOdometer = endOdometer;
		this.endedAt = endedAt;
		this.finishedAt = finishedAt;
	}

	/**
	 * Deleted trips stay listed, they don't count for the odometer
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

	public boolean isOpen()
	{
		return endOdometer == null;
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

	/**
	 * @return the distance in km, <code>null</code> while the trip is open
	 */
	public Integer getDistance()
	{
		return endOdometer == null ? null : endOdometer - startOdometer;
	}

	public UUID getId()
	{
		return id;
	}

	public CarEntity getCar()
	{
		return car;
	}

	public AccountEntity getDriver()
	{
		return driver;
	}

	public TripType getType()
	{
		return type;
	}

	public int getStartOdometer()
	{
		return startOdometer;
	}

	public Integer getEndOdometer()
	{
		return endOdometer;
	}

	public OffsetDateTime getStartedAt()
	{
		return startedAt;
	}

	public OffsetDateTime getEndedAt()
	{
		return endedAt;
	}

	public String getDestination()
	{
		return destination;
	}

	public void setDestination(String destination)
	{
		this.destination = destination;
	}

	public String getPurpose()
	{
		return purpose;
	}

	public void setPurpose(String purpose)
	{
		this.purpose = purpose;
	}

	public String getBusinessPartner()
	{
		return businessPartner;
	}

	public void setBusinessPartner(String businessPartner)
	{
		this.businessPartner = businessPartner;
	}

	public TripReview getReviewReason()
	{
		return reviewReason;
	}

	public void setReviewReason(TripReview reviewReason)
	{
		this.reviewReason = reviewReason;
	}

	public boolean isRestored()
	{
		return restored;
	}

	public OffsetDateTime getCreatedAt()
	{
		return createdAt;
	}

	public OffsetDateTime getFinishedAt()
	{
		return finishedAt;
	}
}
