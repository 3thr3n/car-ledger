package de.codeflowwizardry.carledger.rest.car.access;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.CarRole;

/**
 * An invitation to a car
 *
 * @param account
 *            user name or e-mail address; an account that doesn't exist yet
 *            gets the invitation with its first login
 * @param validFrom
 *            start, from now on without
 * @param validUntil
 *            end (exclusive), without an end without
 */
public record CarInvitationInput(
		@Schema(required = true) String account,
		@Schema(required = true) CarRole role,
		OffsetDateTime validFrom,
		OffsetDateTime validUntil)
{
	public static final int MAX_ACCOUNT = 320;

	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (account == null || account.isBlank())
		{
			errors.add("User name or e-mail address must be set!");
		} else if (account.strip().length() > MAX_ACCOUNT)
		{
			errors.add("User name or e-mail address must not be longer than " + MAX_ACCOUNT + " characters!");
		}
		errors.addAll(access().validate());
		if (validUntil != null && !validUntil.isAfter(OffsetDateTime.now()))
		{
			errors.add("The end must be in the future!");
		}
		return errors;
	}

	public CarAccessInput access()
	{
		return new CarAccessInput(role, validFrom, validUntil);
	}
}
