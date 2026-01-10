package de.codeflowwizardry.carledger.exception;

public abstract class CarLedgerException extends RuntimeException
{
	CarLedgerException(String message, Throwable cause)
	{
		super(message, cause);
	}

	CarLedgerException(String message)
	{
		super(message);
	}
}
