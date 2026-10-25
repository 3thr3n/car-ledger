package de.codeflowwizardry.carledger.rest.car.modification;

import java.time.LocalDate;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.ApprovalType;

/**
 * See {@link ModificationInput}
 *
 * @param bills
 *            the oldest first
 * @param installed
 *            still on the car, without a removed date
 * @param inspectionOpen
 *            installed with an approval that needs an Abnahme, but without its
 *            date
 * @param documents
 *            amount of its documents
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
		@Schema(required = true) List<LinkedBill> bills,
		@Schema(required = true) boolean installed,
		@Schema(required = true) boolean inspectionOpen,
		@Schema(required = true) long documents)
{
}
