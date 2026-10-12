package de.codeflowwizardry.carledger.rest.car.recurring;

/**
 * Order of the recurring bills.
 */
public enum RecurringBillSort {
	/** running bills before finished ones, the newest first */
	ACTIVE,
	/** the next due payment first */
	NEXT_DUE
}
