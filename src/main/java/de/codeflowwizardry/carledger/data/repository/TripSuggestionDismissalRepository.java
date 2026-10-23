package de.codeflowwizardry.carledger.data.repository;

import java.time.OffsetDateTime;
import java.util.List;

import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.TripSuggestionDismissalEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TripSuggestionDismissalRepository implements PanacheRepository<TripSuggestionDismissalEntity>
{
	/**
	 * @return the dismissed drives of the car that end after the time
	 */
	public List<TripSuggestionDismissalEntity> endingAfter(CarEntity car, OffsetDateTime time)
	{
		return list("car = ?1 and endedAt > ?2", car, time);
	}
}
