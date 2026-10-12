package de.codeflowwizardry.carledger.rest.admin;

/**
 * @param activeAccounts
 *            accounts seen in the last 30 days
 */
public record AdminStats(long accounts, long activeAccounts, long cars, long bills, long recurringBills)
{
}
