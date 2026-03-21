package de.codeflowwizardry.carledger.exception;

public class WrongUserException extends CarLedgerException
{
	public WrongUserException()
	{
		super("Car cannot be found under your user!");
	}
}
