package de.codeflowwizardry.carledger.rest.car.track;

import java.util.ArrayList;
import java.util.List;

/**
 * @param days
 *            the GPS track points are kept, forever without
 */
public record TrackRetentionInput(Integer days)
{
	/** ten years */
	public static final int MAX_DAYS = 3650;

	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (days != null && (days < 1 || days > MAX_DAYS))
		{
			errors.add("The track points must be kept for 1 to " + MAX_DAYS + " days, or forever!");
		}
		return errors;
	}
}
