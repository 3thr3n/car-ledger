package de.codeflowwizardry.carledger.rest.admin;

public record MaxCarsInput(Integer maxCars)
{
	public static final int LIMIT = 100;
}
