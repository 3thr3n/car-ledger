package de.codeflowwizardry.carledger.data;

/**
 * Whether the invited account took the access to a car
 */
public enum CarAccessState {
	/** invited, not answered yet; gives no access */
	PENDING,
	/** the access counts within its period */
	ACCEPTED,
	/** stays for the owner to see, a new invitation asks again */
	DECLINED
}
