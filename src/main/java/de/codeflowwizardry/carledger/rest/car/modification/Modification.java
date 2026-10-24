package de.codeflowwizardry.carledger.rest.car.modification;

import java.time.LocalDate;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.ApprovalType;

/**
 * See {@link ModificationInput}
 *
 * @param billId
 *            missing without one or after it was deleted
 * @param installed
 *            still on the car, without a removed date
 * @param inspectionOpen
 *            installed with an approval that needs an Abnahme, but without its
 *            date
 */
public record Modification(
		@Schema(required = true) long id,
		@Schema(required = true) long carId,
		@Schema(required = true) String part,
		String manufacturer,
		@Schema(required = true) ApprovalType approvalType,
		String approvalNumber,
		String conditions,
		LocalDate inspectionDate,
		String testingBody,
		@Schema(required = true) boolean registered,
		LocalDate installedDate,
		Integer installedOdometer,
		LocalDate removedDate,
		Integer removedOdometer,
		String notes,
		Long billId,
		@Schema(required = true) boolean installed,
		@Schema(required = true) boolean inspectionOpen)
{
}
