package de.codeflowwizardry.carledger.rest.car.trip;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * What the UI needs to start or finish a trip, also kept for offline
 *
 * @param open
 *            the open trip of the car, missing if there is none
 * @param minimumOdometer
 *            the lowest start of the next trip: the highest reading of the car
 *            (its odometer, maintenance bills, trips) in km
 * @param estimatedOdometer
 *            the current odometer with the distances of the refuels, only a
 *            hint: a start below it is allowed
 */
public record TripCurrent(
		Trip open,
		@Schema(required = true) int minimumOdometer,
		@Schema(required = true) int estimatedOdometer)
{
}
