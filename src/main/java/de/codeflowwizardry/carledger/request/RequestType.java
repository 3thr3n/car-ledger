package de.codeflowwizardry.carledger.request;

/**
 * What a user requests, the payload of the request depends on it
 */
public enum RequestType {
	/** more cars than the limit of the account, payload: {@link CarLimitPayload} */
	CAR_LIMIT
}
