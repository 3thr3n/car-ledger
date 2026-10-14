package de.codeflowwizardry.carledger.rest.request;

/**
 * @param cars
 *            how many cars more
 * @param reason
 *            optional, shown to the administrator
 */
public record CarRequestInput(Integer cars, String reason)
{
}
