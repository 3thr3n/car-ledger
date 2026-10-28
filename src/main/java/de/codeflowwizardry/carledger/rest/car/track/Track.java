package de.codeflowwizardry.carledger.rest.car.track;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * The GPS track of a car in a time range, simplified for the map
 *
 * @param segments
 *            the points in the order of time, split at gaps of more than 5
 *            minutes without a point; a segment has at least one point
 * @param distance
 *            km, from the raw points within the segments (not over the gaps)
 * @param points
 *            amount of raw points in the range, within the viewport with one
 * @param tolerance
 *            meters the simplified lines may be off the points; the map asks
 *            for
 *            more detail when that is more than a few pixels
 */
public record Track(
		@Schema(required = true) List<List<TrackPoint>> segments,
		@Schema(required = true) double distance,
		@Schema(required = true) int points,
		@Schema(required = true) double tolerance)
{
}
