package de.codeflowwizardry.carledger.data;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;

/**
 * A drive of the GPS tracks the owner doesn't want as a trip
 */
@Entity(name = "trip_suggestion_dismissal")
public class TripSuggestionDismissalEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(insertable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ts_car_id", nullable = false, updatable = false)
	private CarEntity car;

	@Column(name = "ts_started_at", nullable = false, updatable = false)
	private OffsetDateTime startedAt;

	@Column(name = "ts_ended_at", nullable = false, updatable = false)
	private OffsetDateTime endedAt;

	@Column(name = "ts_dismissed_at", nullable = false, updatable = false)
	private OffsetDateTime dismissedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

	protected TripSuggestionDismissalEntity()
	{
	}

	public TripSuggestionDismissalEntity(CarEntity car, OffsetDateTime startedAt, OffsetDateTime endedAt)
	{
		this.car = car;
		this.startedAt = startedAt;
		this.endedAt = endedAt;
	}

	public OffsetDateTime getStartedAt()
	{
		return startedAt;
	}

	public OffsetDateTime getEndedAt()
	{
		return endedAt;
	}
}
