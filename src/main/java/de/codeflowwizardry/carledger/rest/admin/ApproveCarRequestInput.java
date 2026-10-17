package de.codeflowwizardry.carledger.rest.admin;

/**
 * @param maxCars
 *            the new limit, without one the current limit plus the requested
 *            cars
 */
public record ApproveCarRequestInput(Integer maxCars)
{
}
