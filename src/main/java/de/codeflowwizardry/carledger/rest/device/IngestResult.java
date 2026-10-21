package de.codeflowwizardry.carledger.rest.device;

/**
 * @param accepted
 *            points stored
 * @param duplicates
 *            points the tracker sent before (same time), not stored again
 * @param rejected
 *            invalid points, skipped
 */
public record IngestResult(int accepted, int duplicates, int rejected)
{
}
