package de.codeflowwizardry.carledger.data;

/**
 * Why a trip doesn't fit the other data of the car. A live request is rejected
 * with it, a trip from the offline queue is stored with it.
 */
public enum TripReview {
	/** the start is below a reading before it */
	ODOMETER_BELOW_PREVIOUS,
	/** the trip ends above the start of a later trip */
	ODOMETER_ABOVE_NEXT,
	/** the time of the trip overlaps another trip */
	OVERLAPS,
	/** the odometer was reset during the trip */
	ODOMETER_RESET
}
