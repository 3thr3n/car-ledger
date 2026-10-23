package de.codeflowwizardry.carledger.rest.car.trip;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A suggested trip that isn't one, by its times
 */
public record TripSuggestionDismissInput(OffsetDateTime startedAt, OffsetDateTime endedAt)
{
	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (startedAt == null || endedAt == null)
		{
			errors.add("Start and end time must be set!");
		} else if (!endedAt.isAfter(startedAt))
		{
			errors.add("End time must be after the start time!");
		}
		return errors;
	}
}
