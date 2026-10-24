package de.codeflowwizardry.carledger.backup;

/**
 * What a restore created
 */
public record BackupSummary(int cars, int bills, int recurringBills, int reminders, int trips, int files,
		int trackers, int trackPoints, int modifications)
{
}
