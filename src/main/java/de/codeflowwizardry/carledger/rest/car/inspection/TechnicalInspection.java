package de.codeflowwizardry.carledger.rest.car.inspection;

import java.time.LocalDate;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.InspectionResult;

/**
 * See {@link TechnicalInspectionInput}
 *
 * @param nextDue
 *            the first day of the month
 * @param passed
 *            the car got the plaque
 * @param documents
 *            amount of its files, e.g. the report
 */
public record TechnicalInspection(
		@Schema(required = true) long id,
		@Schema(required = true) long carId,
		@Schema(required = true) LocalDate date,
		Integer odometer,
		String testingBody,
		@Schema(required = true) InspectionResult result,
		String defects,
		LocalDate nextDue,
		@Schema(required = true) boolean passed,
		@Schema(required = true) long documents)
{
}
