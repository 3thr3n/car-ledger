package de.codeflowwizardry.carledger.data;

/**
 * What an account may do with a car (see
 * {@link de.codeflowwizardry.carledger.data.repository.CarAccess})
 */
public enum CarRole {
	/** everything, also deleting the car and managing the access */
	OWNER,
	/** sees everything, adds, changes and deletes entries */
	EDITOR,
	/** sees everything, changes nothing */
	VIEWER,
	/** sees the car, drives it: only the own trips and refuels */
	DRIVER
}
