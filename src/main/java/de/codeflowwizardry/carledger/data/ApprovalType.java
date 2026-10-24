package de.codeflowwizardry.carledger.data;

/**
 * How a modification of a car is approved in Germany
 */
public enum ApprovalType {
	/** Allgemeine Betriebserlaubnis of the part, no inspection */
	ABE,
	/** Teilegutachten, needs an Änderungsabnahme (§19.3 StVZO) */
	PARTS_REPORT,
	/** Einzelabnahme of the car with the part (§21 StVZO) */
	SINGLE_APPROVAL,
	/** EG type approval or E-mark of the part */
	EC_TYPE_APPROVAL,
	/** the part needs no approval */
	NONE;

	/**
	 * @return whether a testing body has to inspect the car with the part
	 */
	public boolean needsInspection()
	{
		return this == PARTS_REPORT || this == SINGLE_APPROVAL;
	}
}
