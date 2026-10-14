package de.codeflowwizardry.carledger.backup;

/**
 * What a restore created
 */
public record BackupSummary(int cars, int bills, int recurringBills, int reminders, int files)
{
}
