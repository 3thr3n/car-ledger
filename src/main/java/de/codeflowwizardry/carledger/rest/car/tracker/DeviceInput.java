package de.codeflowwizardry.carledger.rest.car.tracker;

import java.util.ArrayList;
import java.util.List;

/**
 * @param name
 *            to tell the trackers of a car apart
 */
public record DeviceInput(String name)
{
	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (name == null || name.isBlank())
		{
			errors.add("Name must be set!");
		} else if (name.length() > 100)
		{
			errors.add("Name must not be longer than 100 characters!");
		}
		return errors;
	}
}
