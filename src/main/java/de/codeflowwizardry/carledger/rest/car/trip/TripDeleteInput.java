package de.codeflowwizardry.carledger.rest.car.trip;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * @param reason
 *            why the trip is deleted, it stays listed with it
 */
public record TripDeleteInput(String reason)
{
	public static final int MAX_REASON = 500;

	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (StringUtils.isBlank(reason))
		{
			errors.add("A reason must be given!");
		} else if (reason.strip().length() > MAX_REASON)
		{
			errors.add("The reason must not be longer than " + MAX_REASON + " characters!");
		}
		return errors;
	}
}
