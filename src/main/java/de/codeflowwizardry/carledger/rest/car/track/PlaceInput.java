package de.codeflowwizardry.carledger.rest.car.track;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * The name of a place
 *
 * @param lat
 *            where the place is, only to name a new one
 * @param lon
 *            where the place is, only to name a new one
 */
public record PlaceInput(Double lat, Double lon, String name)
{
	public static final int MAX_NAME = 100;

	/**
	 * @param position
	 *            whether the position is needed, for a new name
	 * @return the errors, empty if the input is valid
	 */
	public List<String> validate(boolean position)
	{
		List<String> errors = new ArrayList<>();
		if (StringUtils.isBlank(name))
		{
			errors.add("The name must be set!");
		} else if (name.strip().length() > MAX_NAME)
		{
			errors.add("The name must not be longer than " + MAX_NAME + " characters!");
		}
		if (position && (lat == null || lon == null || Math.abs(lat) > 90 || Math.abs(lon) > 180))
		{
			errors.add("The position of the place is missing or invalid!");
		}
		return errors;
	}
}
