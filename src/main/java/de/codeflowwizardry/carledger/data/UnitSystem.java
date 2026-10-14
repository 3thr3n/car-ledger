package de.codeflowwizardry.carledger.data;

/**
 * Units the values are shown and entered in. Stored and sent by the API are
 * always km, liters and cent per liter.
 */
public enum UnitSystem {
	/** km, liters, l/100km */
	METRIC,
	/** miles, US gallons, mpg (US) */
	US,
	/** miles, liters, mpg (imperial) */
	UK
}
