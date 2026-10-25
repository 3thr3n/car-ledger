package de.codeflowwizardry.carledger.rest.car.track;

import java.time.OffsetDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A piece of road the car passed equally often, simplified for the map
 *
 * @param passes
 *            times the car passed it in the range, both directions
 * @param last
 *            when it passed last
 * @param coordinates
 *            longitude and latitude of each point, as in GeoJSON; at least two
 */
public record FrequencyLine(
		@Schema(required = true) int passes,
		@Schema(required = true) OffsetDateTime last,
		@Schema(required = true) double[][] coordinates)
{
}
