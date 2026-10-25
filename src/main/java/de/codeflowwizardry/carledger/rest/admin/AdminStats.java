package de.codeflowwizardry.carledger.rest.admin;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @param activeAccounts
 *            accounts seen in the last 30 days
 * @param openRequests
 *            requests for more cars waiting for a decision
 * @param fileStorage
 *            the storage of new files
 * @param files
 *            the attachments per storage, storages without any are missing
 */
public record AdminStats(long accounts, long activeAccounts, long cars, long bills, long recurringBills,
		long openRequests, @Schema(required = true) String fileStorage,
		@Schema(required = true) List<AdminFileStorage> files)
{
}
