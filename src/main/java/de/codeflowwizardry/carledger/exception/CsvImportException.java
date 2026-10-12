package de.codeflowwizardry.carledger.exception;

/**
 * A problem of a CSV import the user can fix, e.g. a value that cannot be
 * parsed or a missing column. Only these messages are shown to the user, other
 * exceptions of the import may contain internals and stay in the log.
 */
public class CsvImportException extends CarLedgerException
{
	private final String userMessage;

	public CsvImportException(String userMessage)
	{
		super(userMessage);
		this.userMessage = userMessage;
	}

	/**
	 * @return the message for the user, written by CarLedger
	 */
	public String getUserMessage()
	{
		return userMessage;
	}
}
