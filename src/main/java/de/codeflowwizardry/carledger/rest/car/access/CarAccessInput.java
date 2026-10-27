package de.codeflowwizardry.carledger.rest.car.access;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.CarRole;

/**
 * The role and the period of an access to a car
 *
 * @param validFrom
 *            start, from now on without
 * @param validUntil
 *            end (exclusive), without an end without
 */
public record CarAccessInput(
		@Schema(required = true) CarRole role,
		OffsetDateTime validFrom,
		OffsetDateTime validUntil)
{
	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (role == null || role == CarRole.OWNER)
		{
			errors.add("Role must be editor, viewer or driver!");
		}
		if (validFrom != null && validUntil != null && !validUntil.isAfter(validFrom))
		{
			errors.add("The end must be after the start!");
		}
		return errors;
	}
}
