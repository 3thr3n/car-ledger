package de.codeflowwizardry.carledger.data;

/**
 * Result of a technical inspection (HU), the worst first is the last
 */
public enum InspectionResult {
	/** ohne Mängel */
	NO_DEFECTS,
	/** geringe Mängel: the plaque, the defects are repaired at once */
	MINOR_DEFECTS,
	/** erhebliche Mängel: no plaque, a Nachprüfung within a month */
	MAJOR_DEFECTS,
	/** gefährliche Mängel: no plaque, driving only to the workshop */
	DANGEROUS_DEFECTS,
	/** verkehrsunsicher: the car must not be driven */
	UNSAFE;

	/**
	 * @return whether the car got the plaque
	 */
	public boolean passed()
	{
		return this == NO_DEFECTS || this == MINOR_DEFECTS;
	}
}
