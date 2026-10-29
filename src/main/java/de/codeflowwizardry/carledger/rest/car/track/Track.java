package de.codeflowwizardry.carledger.rest.car.track;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * The GPS track of a car in a time range, simplified for the map
 *
 * @param segments
 *            the points in the order of time, split at gaps of more than 5
 *            minutes without a point and between selected drives; on the shared
 *            line of the streets driven more than once, simplified with 1 m:
 *            the
 *            map simplifies further by its zoom
 * @param distance
 *            km, from the raw points within the segments (not over the gaps)
 * @param points
 *            amount of raw points in the range
 */
public record Track(
		@Schema(required = true) List<TrackSegment> segments,
		@Schema(required = true) double distance,
		@Schema(required = true) int points)
{
}
