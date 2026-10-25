package de.codeflowwizardry.carledger.rest.car.track;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * How often the car drove the roads of its track in a time range
 *
 * @param lines
 *            the track cut where the amount of passes changes, the most
 *            frequent last: drawn on top
 * @param maxPasses
 *            of the lines, 0 without one
 */
public record TrackFrequency(
		@Schema(required = true) List<FrequencyLine> lines,
		@Schema(required = true) int maxPasses)
{
}
