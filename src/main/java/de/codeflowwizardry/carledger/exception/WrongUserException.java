package de.codeflowwizardry.carledger.exception;

public class WrongUserException extends CarLedgerException
{
	public WrongUserException(String message)
	{
		super(message);
	}

	public WrongUserException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
