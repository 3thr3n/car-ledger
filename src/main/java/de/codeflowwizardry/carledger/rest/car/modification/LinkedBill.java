package de.codeflowwizardry.carledger.rest.car.modification;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;

/**
 * A bill of a modification, e.g. the part, the workshop or the Abnahme
 *
 * @param description
 *            of a maintenance or another entry
 */
public record LinkedBill(
		@Schema(required = true) long id,
		@Schema(required = true) BillType type,
		@Schema(required = true) LocalDate date,
		@Schema(required = true) BigDecimal total,
		String description)
{
	public static LinkedBill convert(BillEntity bill)
	{
		String description = bill.getMaintenanceBill() != null ? bill.getMaintenanceBill().getDescription()
				: bill.getMiscellaneousBill() != null ? bill.getMiscellaneousBill().getDescription() : null;
		return new LinkedBill(bill.getId(), bill.getType(), bill.getDate(), bill.getTotal(), description);
	}
}
