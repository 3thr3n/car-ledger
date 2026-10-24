package de.codeflowwizardry.carledger.data;

/**
 * What a document of a modification is, in the order they are listed
 */
public enum DocumentKind {
	/** the Gutachten, the ABE or another approval of the part */
	APPROVAL,
	/** the certificate of the Abnahme */
	INSPECTION, PHOTO, OTHER
}
