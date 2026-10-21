package de.codeflowwizardry.carledger.rest.car.tracker;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A new GPS tracker
 *
 * @param token
 *            for the tracker, <code>Authorization: Bearer</code>. Only this
 *            answer contains it, it can't be shown again.
 */
public record DeviceCreated(
		@Schema(required = true) Device device,
		@Schema(required = true) String token)
{
}
