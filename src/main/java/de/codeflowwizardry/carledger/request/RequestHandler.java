package de.codeflowwizardry.carledger.request;

/**
 * What the approval of a request of one type changes, e.g. the car limit
 *
 * @param <I>
 *            what the administrator decides with the approval
 */
public interface RequestHandler<I>
{
	RequestType type();

	/**
	 * Applies the approved request, in the transaction of the approval
	 *
	 * @throws jakarta.ws.rs.BadRequestException
	 *             if the input is invalid
	 */
	void approve(RequestEntity request, I input);
}
