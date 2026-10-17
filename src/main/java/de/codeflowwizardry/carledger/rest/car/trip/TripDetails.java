package de.codeflowwizardry.carledger.rest.car.trip;

import java.util.List;

/**
 * Details of the Fahrtenbuch, given at the start or at the finish of a trip
 */
interface TripDetails
{
	int MAX_DESTINATION = 200;
	int MAX_PURPOSE = 500;
	int MAX_BUSINESS_PARTNER = 200;

	String destination();

	String purpose();

	String businessPartner();

	default void validateDetails(List<String> errors)
	{
		if (destination() != null && destination().length() > MAX_DESTINATION)
		{
			errors.add("Destination must not be longer than " + MAX_DESTINATION + " characters!");
		}
		if (purpose() != null && purpose().length() > MAX_PURPOSE)
		{
			errors.add("Purpose must not be longer than " + MAX_PURPOSE + " characters!");
		}
		if (businessPartner() != null && businessPartner().length() > MAX_BUSINESS_PARTNER)
		{
			errors.add("Business partner must not be longer than " + MAX_BUSINESS_PARTNER + " characters!");
		}
	}
}
