package de.codeflowwizardry.carledger.rest.car.track;

import java.time.OffsetDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A place the car stops at often, or one its owner named
 *
 * @param id
 *            of the name, missing for a place without one
 * @param name
 *            e.g. "Fuel station", missing until the owner names it
 * @param visits
 *            stops in the range, 0 for a named place not visited in it
 * @param lastVisit
 *            the latest stop in the range, missing without one
 */
public record Place(
		Long id,
		String name,
		@Schema(required = true) double lat,
		@Schema(required = true) double lon,
		@Schema(required = true) int visits,
		OffsetDateTime lastVisit)
{
}
