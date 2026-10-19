package de.codeflowwizardry.carledger.rest.car.trip;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Body of a 409 for a trip
 *
 * @param minimumOdometer
 *            the lowest allowed odometer, for
 *            {@link Reason#ODOMETER_BELOW_PREVIOUS}
 * @param maximumOdometer
 *            the highest allowed odometer, for
 *            {@link Reason#ODOMETER_ABOVE_NEXT}
 */
public record TripConflict(
		@Schema(required = true) Reason reason,
		@Schema(required = true) String message,
		Integer minimumOdometer,
		Integer maximumOdometer)
{
	public enum Reason {
		/** see {@link de.codeflowwizardry.carledger.data.TripReview} */
		ODOMETER_BELOW_PREVIOUS, ODOMETER_ABOVE_NEXT, OVERLAPS,
		/** another trip of the car is open */
		OPEN_TRIP,
		/** the id belongs to a trip with other values, or to another car */
		ID_IN_USE,
		/** the trip was finished with other values */
		ALREADY_FINISHED,
		/** a detail given at the start can't be changed */
		DETAIL_CHANGED,
		/** the open trip was deleted, it can't be finished */
		DELETED
	}
}
